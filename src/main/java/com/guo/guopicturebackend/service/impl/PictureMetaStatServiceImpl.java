package com.guo.guopicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.guo.guopicturebackend.service.PictureCategoryService;
import com.guo.guopicturebackend.service.PictureMetaStatService;
import com.guo.guopicturebackend.service.PictureTagService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PictureMetaStatServiceImpl implements PictureMetaStatService {

    @Resource
    private PictureTagService pictureTagService;

    @Resource
    private PictureCategoryService pictureCategoryService;

    @Override
    public void applyPictureMetadataDelta(String oldCategory, String oldTagsJson,
                                          String newCategory, String newTagsJson) {
        String oc = normalizeCategory(oldCategory);
        String nc = normalizeCategory(newCategory);
        if (!java.util.Objects.equals(oc, nc)) {
            if (StrUtil.isNotBlank(oc)) {
                pictureCategoryService.decrementUsageIfRegistered(oc);
            }
            if (StrUtil.isNotBlank(nc)) {
                pictureCategoryService.incrementUsageIfRegistered(nc);
            }
        }
        List<String> oldTags = parseDistinctTags(oldTagsJson);
        List<String> newTags = parseDistinctTags(newTagsJson);
        for (String t : oldTags) {
            if (!newTags.contains(t)) {
                pictureTagService.decrementUsageIfRegistered(t);
            }
        }
        for (String t : newTags) {
            if (!oldTags.contains(t)) {
                pictureTagService.incrementUsageIfRegistered(t);
            }
        }
    }

    private static String normalizeCategory(String c) {
        if (c == null) {
            return null;
        }
        String t = c.trim();
        return t.isEmpty() ? null : t;
    }

    private static List<String> parseDistinctTags(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return JSONUtil.toList(json, String.class).stream()
                    .filter(StrUtil::isNotBlank)
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
