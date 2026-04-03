package com.guo.guopicturebackend.outpainting;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guo.guopicturebackend.api.aliyun.AliYunAiApi;
import com.guo.guopicturebackend.api.aliyun.model.GetOutPaintingTaskResponse;
import com.guo.guopicturebackend.exception.BusinessException;
import com.guo.guopicturebackend.exception.ErrorCode;
import com.guo.guopicturebackend.mapper.PictureOutpaintTaskMapper;
import com.guo.guopicturebackend.model.dto.picture.PictureOutpaintTaskQueryRequest;
import com.guo.guopicturebackend.model.entity.PictureOutpaintTask;
import com.guo.guopicturebackend.model.entity.User;
import com.guo.guopicturebackend.model.vo.PictureOutpaintTaskVO;
import com.guo.guopicturebackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class OutPaintingTaskQueryService {

    private final PictureOutpaintTaskMapper pictureOutpaintTaskMapper;
    private final AliYunAiApi aliYunAiApi;
    private final UserService userService;
    private final OutPaintingTaskProcessor outPaintingTaskProcessor;

    public PictureOutpaintTaskVO getTaskVo(Long taskId, User loginUser) {
        PictureOutpaintTask task = pictureOutpaintTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务不存在");
        }
        if (!task.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        if (PictureOutpaintTaskStatus.RUNNING.equals(task.getStatus())
                || PictureOutpaintTaskStatus.RECONCILING.equals(task.getStatus())) {
            refreshFromAliyun(task);
            task = pictureOutpaintTaskMapper.selectById(taskId);
        }
        return toVo(task);
    }

    /**
     * 根据阿里云查询结果更新本地任务（成功保留额度；失败退款）
     */
    public void refreshFromAliyun(PictureOutpaintTask task) {
        if (task == null || PictureOutpaintTaskStatus.isTerminal(task.getStatus())) {
            return;
        }
        String aliyunId = task.getAliyunTaskId();
        if (aliyunId == null) {
            return;
        }
        GetOutPaintingTaskResponse resp;
        try {
            resp = aliYunAiApi.getOutPaintingTask(aliyunId);
        } catch (Exception e) {
            return;
        }
        if (resp == null || resp.getOutput() == null) {
            return;
        }
        GetOutPaintingTaskResponse.Output out = resp.getOutput();
        String st = out.getTaskStatus();
        Date now = new Date();
        if ("SUCCEEDED".equalsIgnoreCase(st)) {
            pictureOutpaintTaskMapper.update(null,
                    new LambdaUpdateWrapper<PictureOutpaintTask>()
                            .eq(PictureOutpaintTask::getId, task.getId())
                            .in(PictureOutpaintTask::getStatus,
                                    PictureOutpaintTaskStatus.RUNNING, PictureOutpaintTaskStatus.RECONCILING)
                            .set(PictureOutpaintTask::getStatus, PictureOutpaintTaskStatus.SUCCEEDED)
                            .set(PictureOutpaintTask::getOutputImageUrl, out.getOutputImageUrl())
                            .set(PictureOutpaintTask::getFinishTime, now)
                            .set(PictureOutpaintTask::getUpdateTime, now));
            return;
        }
        if ("FAILED".equalsIgnoreCase(st) || "UNKNOWN".equalsIgnoreCase(st)) {
            String code = out.getCode();
            String userMsg = OutPaintingDashScopeErrorMapper.toUserMessage(code, out.getMessage());
            boolean updated = pictureOutpaintTaskMapper.update(null,
                    new LambdaUpdateWrapper<PictureOutpaintTask>()
                            .eq(PictureOutpaintTask::getId, task.getId())
                            .in(PictureOutpaintTask::getStatus,
                                    PictureOutpaintTaskStatus.RUNNING, PictureOutpaintTaskStatus.RECONCILING)
                            .set(PictureOutpaintTask::getStatus, PictureOutpaintTaskStatus.FAILED)
                            .set(PictureOutpaintTask::getFinishTime, now)
                            .set(PictureOutpaintTask::getErrorCode, code)
                            .set(PictureOutpaintTask::getErrorMessage, userMsg)
                            .set(PictureOutpaintTask::getRawError, out.getMessage())
                            .set(PictureOutpaintTask::getUpdateTime, now)) > 0;
            if (updated) {
                outPaintingTaskProcessor.refundOnce(task.getId());
            }
        }
    }

    public Page<PictureOutpaintTaskVO> pageTasks(PictureOutpaintTaskQueryRequest q, User loginUser) {
        long current = q.getCurrent();
        long size = q.getPageSize();
        LambdaQueryWrapper<PictureOutpaintTask> w = new LambdaQueryWrapper<>();
        if (userService.isAdmin(loginUser) && q.getUserId() != null && q.getUserId() > 0) {
            w.eq(PictureOutpaintTask::getUserId, q.getUserId());
        } else {
            w.eq(PictureOutpaintTask::getUserId, loginUser.getId());
        }
        w.orderByDesc(PictureOutpaintTask::getCreateTime);
        Page<PictureOutpaintTask> page = pictureOutpaintTaskMapper.selectPage(new Page<>(current, size), w);
        Page<PictureOutpaintTaskVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVo).collect(java.util.stream.Collectors.toList()));
        return voPage;
    }

    private PictureOutpaintTaskVO toVo(PictureOutpaintTask t) {
        if (t == null) {
            return null;
        }
        PictureOutpaintTaskVO vo = new PictureOutpaintTaskVO();
        BeanUtils.copyProperties(t, vo);
        return vo;
    }
}
