package com.guo.guopicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.guo.guopicturebackend.config.HunyuanProperties;
import com.guo.guopicturebackend.manager.HunyuanManager;
import com.guo.guopicturebackend.mapper.PictureMapper;
import com.guo.guopicturebackend.model.dto.hunyuan.PictureModerationResult;
import com.guo.guopicturebackend.model.entity.Picture;
import com.guo.guopicturebackend.model.enums.PictureReviewStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 上传后异步：混元多模态看图做内容初审（与人工审核串联）
 */
@Service
@Slf4j
public class PictureAiModerationService {

    /** AI 尚未返回或调用失败 */
    public static final int AI_STATUS_PENDING = 0;
    /** AI 判定可进入人工审核队列 */
    public static final int AI_STATUS_PASS = 1;
    /** AI 判定不适合展示，直接拒绝（仍可被管理员改状态，需另接口） */
    public static final int AI_STATUS_REJECT = 2;

    @Resource
    private HunyuanProperties hunyuanProperties;

    @Resource
    private HunyuanManager hunyuanManager;

    @Resource
    private PictureMapper pictureMapper;

    @Async
    public void scheduleAfterUpload(Long pictureId, Long spaceId, String imageUrl) {
        if (!hunyuanProperties.isEnabled() || !hunyuanProperties.isAiReviewEnabled()) {
            return;
        }
        if (pictureId == null || StrUtil.isBlank(imageUrl)) {
            return;
        }
        long sid = spaceId != null ? spaceId : 0L;
        try {
            PictureModerationResult result = hunyuanManager.moderatePictureByUrl(imageUrl);
            if (result == null || result.getPass() == null) {
                log.warn("图片 AI 审核无有效结果，保持待人工审核 pictureId={} spaceId={}", pictureId, sid);
                return;
            }
            String reason = StrUtil.blankToDefault(StrUtil.trim(result.getReason()), "未说明");
            Date now = new Date();
            if (Boolean.FALSE.equals(result.getPass())) {
                pictureMapper.update(null, new LambdaUpdateWrapper<Picture>()
                        .eq(Picture::getId, pictureId)
                        .eq(Picture::getSpaceId, sid)
                        .set(Picture::getAiReviewStatus, AI_STATUS_REJECT)
                        .set(Picture::getAiReviewMessage, reason)
                        .set(Picture::getReviewStatus, PictureReviewStatusEnum.REJECT.getValue())
                        .set(Picture::getReviewMessage, "[AI审核] " + reason)
                        .set(Picture::getReviewTime, now)
                        .set(Picture::getEditTime, now));
            } else {
                pictureMapper.update(null, new LambdaUpdateWrapper<Picture>()
                        .eq(Picture::getId, pictureId)
                        .eq(Picture::getSpaceId, sid)
                        .set(Picture::getAiReviewStatus, AI_STATUS_PASS)
                        .set(Picture::getAiReviewMessage, reason)
                        .set(Picture::getEditTime, now));
            }
        } catch (Exception e) {
            log.error("图片 AI 审核异常 pictureId={} spaceId={}", pictureId, sid, e);
        }
    }
}
