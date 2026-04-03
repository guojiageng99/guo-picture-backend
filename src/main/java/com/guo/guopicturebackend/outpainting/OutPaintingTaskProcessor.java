package com.guo.guopicturebackend.outpainting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.guo.guopicturebackend.api.aliyun.AliYunAiApi;
import com.guo.guopicturebackend.api.aliyun.model.CreateOutPaintingTaskRequest;
import com.guo.guopicturebackend.api.aliyun.model.CreateOutPaintingTaskResponse;
import com.guo.guopicturebackend.exception.BusinessException;
import com.guo.guopicturebackend.mapper.PictureOutpaintTaskMapper;
import com.guo.guopicturebackend.model.dto.picture.CreatePictureOutPaintingTaskRequest;
import com.guo.guopicturebackend.model.entity.Picture;
import com.guo.guopicturebackend.model.entity.PictureOutpaintTask;
import com.guo.guopicturebackend.service.PictureService;
import com.guo.guopicturebackend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

/**
 * MQ / 线程池消费：向阿里云提交扩图任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutPaintingTaskProcessor {

    private final PictureOutpaintTaskMapper pictureOutpaintTaskMapper;
    private final AliYunAiApi aliYunAiApi;
    private final PictureService pictureService;
    private final UserService userService;

    @Transactional(rollbackFor = Exception.class)
    public void processSubmit(Long taskId) {
        PictureOutpaintTask task = pictureOutpaintTaskMapper.selectById(taskId);
        if (task == null || !PictureOutpaintTaskStatus.PENDING.equals(task.getStatus())) {
            return;
        }
        CreatePictureOutPaintingTaskRequest req;
        try {
            req = JSONUtil.toBean(task.getRequestJson(), CreatePictureOutPaintingTaskRequest.class);
        } catch (Exception e) {
            log.warn("outpaint task request json invalid id={}", taskId, e);
            failFromStatus(taskId, PictureOutpaintTaskStatus.PENDING, "任务参数异常，已退回扩图额度",
                    "BAD_REQUEST", e.getMessage());
            return;
        }
        Long pictureId = req.getPictureId();
        Long spaceId = req.getSpaceId() != null ? req.getSpaceId() : 0L;
        Picture picture = Optional.ofNullable(pictureService.getPictureByIdAndSpaceId(pictureId, spaceId))
                .orElse(null);
        if (picture == null) {
            failFromStatus(taskId, PictureOutpaintTaskStatus.PENDING, "图片不存在，已退回扩图额度",
                    "NOT_FOUND", null);
            return;
        }
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        taskRequest.setInput(input);
        BeanUtil.copyProperties(req, taskRequest);
        try {
            CreateOutPaintingTaskResponse response = aliYunAiApi.createOutPaintingTask(taskRequest);
            String aliyunId = response.getOutput() != null ? response.getOutput().getTaskId() : null;
            if (aliyunId == null) {
                failFromStatus(taskId, PictureOutpaintTaskStatus.PENDING, "扩图服务未返回任务编号，已退回扩图额度",
                        "NO_TASK_ID", JSONUtil.toJsonStr(response));
                return;
            }
            Date now = new Date();
            boolean updated = pictureOutpaintTaskMapper.update(null,
                    new LambdaUpdateWrapper<PictureOutpaintTask>()
                            .eq(PictureOutpaintTask::getId, taskId)
                            .eq(PictureOutpaintTask::getStatus, PictureOutpaintTaskStatus.PENDING)
                            .set(PictureOutpaintTask::getStatus, PictureOutpaintTaskStatus.RUNNING)
                            .set(PictureOutpaintTask::getAliyunTaskId, aliyunId)
                            .set(PictureOutpaintTask::getAliSubmittedAt, now)
                            .set(PictureOutpaintTask::getUpdateTime, now)) > 0;
            if (!updated) {
                log.warn("outpaint transition PENDING->RUNNING lost race id={}", taskId);
            }
        } catch (BusinessException e) {
            failFromStatus(taskId, PictureOutpaintTaskStatus.PENDING,
                    e.getMessage() != null ? e.getMessage() : "扩图任务失败，已退回扩图额度",
                    "BUSINESS", null);
        } catch (Exception e) {
            log.error("outpaint submit unexpected id={}", taskId, e);
            failFromStatus(taskId, PictureOutpaintTaskStatus.PENDING, "扩图服务异常，已退回扩图额度",
                    "SYSTEM", e.getMessage());
        }
    }

    /**
     * 排队过久（未消费或未提交阿里云）：标记失败并退款
     */
    @Transactional(rollbackFor = Exception.class)
    public void failPendingQueueTimeout(Long taskId) {
        failFromStatus(taskId, PictureOutpaintTaskStatus.PENDING, "任务排队超时，已退回扩图额度",
                "QUEUE_TIMEOUT", null);
    }

    /**
     * 对账结束仍无成功：标记失败并退款
     */
    @Transactional(rollbackFor = Exception.class)
    public void failReconcileTimeout(Long taskId) {
        failFromStatus(taskId, PictureOutpaintTaskStatus.RECONCILING, "处理超时，已退回扩图额度",
                "LOCAL_TIMEOUT", null);
    }

    private void failFromStatus(Long taskId, String expectedStatus, String userMessage,
                                String errorCode, String raw) {
        Date now = new Date();
        boolean updated = pictureOutpaintTaskMapper.update(null,
                new LambdaUpdateWrapper<PictureOutpaintTask>()
                        .eq(PictureOutpaintTask::getId, taskId)
                        .eq(PictureOutpaintTask::getStatus, expectedStatus)
                        .set(PictureOutpaintTask::getStatus, PictureOutpaintTaskStatus.FAILED)
                        .set(PictureOutpaintTask::getFinishTime, now)
                        .set(PictureOutpaintTask::getErrorMessage, userMessage)
                        .set(PictureOutpaintTask::getErrorCode, errorCode)
                        .set(PictureOutpaintTask::getRawError, raw)
                        .set(PictureOutpaintTask::getUpdateTime, now)) > 0;
        if (updated) {
            refundOnce(taskId);
        }
    }

    /**
     * 幂等退款：仅 quotaRefunded=0 时退回用户额度并置 1
     */
    public void refundOnce(Long taskId) {
        PictureOutpaintTask t = pictureOutpaintTaskMapper.selectById(taskId);
        if (t == null) {
            return;
        }
        if (t.getQuotaRefunded() != null && t.getQuotaRefunded() == 1) {
            return;
        }
        int rows = pictureOutpaintTaskMapper.update(null,
                new LambdaUpdateWrapper<PictureOutpaintTask>()
                        .eq(PictureOutpaintTask::getId, taskId)
                        .eq(PictureOutpaintTask::getQuotaRefunded, 0)
                        .set(PictureOutpaintTask::getQuotaRefunded, 1));
        if (rows > 0) {
            userService.refundOutpaintQuota(t.getUserId(), t.getQuotaCost());
        }
    }
}
