package com.guo.guopicturebackend.outpainting;

import cn.hutool.json.JSONUtil;
import com.guo.guopicturebackend.api.aliyun.model.CreateOutPaintingTaskRequest;
import com.guo.guopicturebackend.config.OutpaintingProperties;
import com.guo.guopicturebackend.exception.BusinessException;
import com.guo.guopicturebackend.exception.ErrorCode;
import com.guo.guopicturebackend.mapper.PictureOutpaintTaskMapper;
import com.guo.guopicturebackend.model.dto.picture.CreatePictureOutPaintingTaskRequest;
import com.guo.guopicturebackend.model.entity.Picture;
import com.guo.guopicturebackend.model.entity.PictureOutpaintTask;
import com.guo.guopicturebackend.model.entity.User;
import com.guo.guopicturebackend.model.vo.PictureOutpaintSubmitVO;
import com.guo.guopicturebackend.service.PictureService;
import com.guo.guopicturebackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OutPaintingSubmitService {

    private final OutPaintingRateLimiter outPaintingRateLimiter;
    private final OutpaintingProperties outpaintingProperties;
    private final UserService userService;
    private final PictureService pictureService;
    private final PictureOutpaintTaskMapper pictureOutpaintTaskMapper;
    @Resource
    private OutPaintingDispatchService outPaintingDispatchService;

    /**
     * 预扣额度、落库、事务提交后投递 MQ 或线程池
     */
    @Transactional(rollbackFor = Exception.class)
    public PictureOutpaintSubmitVO submit(CreatePictureOutPaintingTaskRequest req, User loginUser) {
        if (req == null || req.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long spaceId = req.getSpaceId() != null ? req.getSpaceId() : 0L;
        outPaintingRateLimiter.checkOrThrow(loginUser.getId());

        int cost = resolveQuotaCost(req.getParameters());
        String modeCode = cost > outpaintingProperties.getQuotaStandardCost() ? "hd" : "standard";

        Picture picture = Optional.ofNullable(pictureService.getPictureByIdAndSpaceId(req.getPictureId(), spaceId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));
        pictureService.checkPictureAuth(loginUser, picture);

        boolean deducted = userService.tryDeductOutpaintQuota(loginUser.getId(), cost);
        if (!deducted) {
            throw new BusinessException(ErrorCode.OUTPAINT_QUOTA_EXHAUSTED);
        }

        Date now = new Date();
        PictureOutpaintTask task = new PictureOutpaintTask();
        task.setUserId(loginUser.getId());
        task.setPictureId(req.getPictureId());
        task.setSpaceId(spaceId);
        task.setStatus(PictureOutpaintTaskStatus.PENDING);
        task.setModeCode(modeCode);
        task.setQuotaCost(cost);
        task.setQuotaRefunded(0);
        task.setRequestJson(JSONUtil.toJsonStr(req));
        task.setReconcileAttempts(0);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        pictureOutpaintTaskMapper.insert(task);

        Long taskId = task.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                outPaintingDispatchService.dispatchAfterCommit(taskId);
            }
        });

        User fresh = userService.getById(loginUser.getId());
        int remaining = fresh != null && fresh.getOutpaintQuota() != null ? fresh.getOutpaintQuota() : 0;
        return PictureOutpaintSubmitVO.builder()
                .id(taskId)
                .status(PictureOutpaintTaskStatus.PENDING)
                .outpaintQuotaRemaining(remaining)
                .build();
    }

    private int resolveQuotaCost(CreateOutPaintingTaskRequest.Parameters parameters) {
        boolean hd = parameters != null && Boolean.TRUE.equals(parameters.getBestQuality());
        return hd ? outpaintingProperties.getQuotaHdCost() : outpaintingProperties.getQuotaStandardCost();
    }
}
