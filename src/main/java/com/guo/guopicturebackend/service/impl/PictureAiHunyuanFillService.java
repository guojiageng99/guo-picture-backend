package com.guo.guopicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.guo.guopicturebackend.config.HunyuanProperties;
import com.guo.guopicturebackend.manager.HunyuanManager;
import com.guo.guopicturebackend.model.dto.hunyuan.ChatTopicInfoCategory;
import com.guo.guopicturebackend.model.entity.Picture;
import com.guo.guopicturebackend.service.PictureMetaStatService;
import com.guo.guopicturebackend.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 上传成功后异步用混元补全图片简介/标签/分类（需图片 URL 对混元公网可访问）
 */
@Service
@Slf4j
public class PictureAiHunyuanFillService {

    @Resource
    private HunyuanProperties hunyuanProperties;

    @Resource
    private HunyuanManager hunyuanManager;

    @Resource
    private PictureService pictureService;

    @Resource
    private PictureMetaStatService pictureMetaStatService;

    @Async
    public void tryFillPictureMetadata(Long pictureId, Long spaceId, String imageUrl) {
        if (!hunyuanProperties.isEnabled() || !hunyuanProperties.isAutoFillAfterUpload()) {
            return;
        }
        if (pictureId == null || StrUtil.isBlank(imageUrl)) {
            return;
        }
        long sid = spaceId != null ? spaceId : 0L;
        try {
            ChatTopicInfoCategory info = hunyuanManager.getChatTopicInfoCategory(imageUrl);
            if (info == null) {
                return;
            }
            Picture current = pictureService.getPictureByIdAndSpaceId(pictureId, sid);
            if (current == null) {
                return;
            }
            String oldCat = current.getCategory();
            String oldTags = current.getTags();

            String newIntro = StrUtil.isNotBlank(info.getIntroduction())
                    ? info.getIntroduction().trim()
                    : current.getIntroduction();
            String newCat = StrUtil.isNotBlank(info.getCategory())
                    ? info.getCategory().trim()
                    : current.getCategory();
            String newTagsJson = current.getTags();
            if (CollUtil.isNotEmpty(info.getTags())) {
                newTagsJson = JSONUtil.toJsonStr(
                        info.getTags().stream()
                                .filter(StrUtil::isNotBlank)
                                .map(String::trim)
                                .distinct()
                                .collect(Collectors.toList()));
            } else if (info.getTags() != null && info.getTags().isEmpty()) {
                newTagsJson = "[]";
            }

            boolean changed = !Objects.equals(newIntro, current.getIntroduction())
                    || !Objects.equals(newCat, current.getCategory())
                    || !Objects.equals(newTagsJson, StrUtil.nullToEmpty(current.getTags()));

            if (!changed) {
                return;
            }

            pictureMetaStatService.applyPictureMetadataDelta(oldCat, oldTags, newCat, newTagsJson);
            boolean ok = pictureService.lambdaUpdate()
                    .eq(Picture::getId, pictureId)
                    .eq(Picture::getSpaceId, sid)
                    .set(Picture::getIntroduction, newIntro)
                    .set(Picture::getCategory, newCat)
                    .set(Picture::getTags, newTagsJson)
                    .set(Picture::getEditTime, new Date())
                    .update();
            if (!ok) {
                log.warn("混元元数据写回失败 pictureId={} spaceId={}", pictureId, sid);
            }
        } catch (Exception e) {
            log.error("混元异步写回异常 pictureId={} spaceId={}", pictureId, sid, e);
        }
    }
}
