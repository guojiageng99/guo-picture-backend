package com.guo.guopicturebackend.outpainting;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.guo.guopicturebackend.config.OutpaintingProperties;
import com.guo.guopicturebackend.mapper.PictureOutpaintTaskMapper;
import com.guo.guopicturebackend.model.entity.PictureOutpaintTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 排队超时、运行超时转对账、对账轮询与最终退款
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutPaintingReconcileScheduler {

    private final PictureOutpaintTaskMapper pictureOutpaintTaskMapper;
    private final OutpaintingProperties outpaintingProperties;
    private final OutPaintingTaskProcessor outPaintingTaskProcessor;
    private final OutPaintingTaskQueryService outPaintingTaskQueryService;

    @Scheduled(fixedDelayString = "${outpainting.scheduler-delay-ms:60000}")
    public void tick() {
        try {
            handleStalePending();
            transitionRunningToReconciling();
            reconcileReconciling();
        } catch (Exception e) {
            log.warn("outpaint scheduler tick error", e);
        }
    }

    private void handleStalePending() {
        Date before = DateUtil.offsetMinute(new Date(), -outpaintingProperties.getLocalTimeoutMinutes());
        List<PictureOutpaintTask> list = pictureOutpaintTaskMapper.selectList(
                new LambdaQueryWrapper<PictureOutpaintTask>()
                        .eq(PictureOutpaintTask::getStatus, PictureOutpaintTaskStatus.PENDING)
                        .lt(PictureOutpaintTask::getCreateTime, before));
        for (PictureOutpaintTask t : list) {
            outPaintingTaskProcessor.failPendingQueueTimeout(t.getId());
        }
    }

    private void transitionRunningToReconciling() {
        Date before = DateUtil.offsetMinute(new Date(), -outpaintingProperties.getLocalTimeoutMinutes());
        List<PictureOutpaintTask> list = pictureOutpaintTaskMapper.selectList(
                new LambdaQueryWrapper<PictureOutpaintTask>()
                        .eq(PictureOutpaintTask::getStatus, PictureOutpaintTaskStatus.RUNNING)
                        .isNotNull(PictureOutpaintTask::getAliSubmittedAt)
                        .lt(PictureOutpaintTask::getAliSubmittedAt, before));
        Date now = new Date();
        for (PictureOutpaintTask t : list) {
            pictureOutpaintTaskMapper.update(null,
                    new LambdaUpdateWrapper<PictureOutpaintTask>()
                            .eq(PictureOutpaintTask::getId, t.getId())
                            .eq(PictureOutpaintTask::getStatus, PictureOutpaintTaskStatus.RUNNING)
                            .set(PictureOutpaintTask::getStatus, PictureOutpaintTaskStatus.RECONCILING)
                            .set(PictureOutpaintTask::getReconcileAttempts, 0)
                            .set(PictureOutpaintTask::getUpdateTime, now));
        }
    }

    private void reconcileReconciling() {
        List<PictureOutpaintTask> list = pictureOutpaintTaskMapper.selectList(
                new LambdaQueryWrapper<PictureOutpaintTask>()
                        .eq(PictureOutpaintTask::getStatus, PictureOutpaintTaskStatus.RECONCILING));
        int max = Math.max(1, outpaintingProperties.getReconcileMaxAttempts());
        for (PictureOutpaintTask t : list) {
            outPaintingTaskQueryService.refreshFromAliyun(t);
            PictureOutpaintTask fresh = pictureOutpaintTaskMapper.selectById(t.getId());
            if (fresh == null || PictureOutpaintTaskStatus.isTerminal(fresh.getStatus())) {
                continue;
            }
            int attempts = fresh.getReconcileAttempts() == null ? 0 : fresh.getReconcileAttempts();
            attempts++;
            pictureOutpaintTaskMapper.update(null,
                    new LambdaUpdateWrapper<PictureOutpaintTask>()
                            .eq(PictureOutpaintTask::getId, fresh.getId())
                            .eq(PictureOutpaintTask::getStatus, PictureOutpaintTaskStatus.RECONCILING)
                            .set(PictureOutpaintTask::getReconcileAttempts, attempts)
                            .set(PictureOutpaintTask::getUpdateTime, new Date()));
            if (attempts >= max) {
                outPaintingTaskProcessor.failReconcileTimeout(fresh.getId());
            }
        }
    }
}
