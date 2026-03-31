package com.guo.guopicturebackend.service;

/**
 * 根据图片分类/标签变更维护字典表 usage_count（仅统计字典中已存在的名称）
 */
public interface PictureMetaStatService {

    void applyPictureMetadataDelta(String oldCategory, String oldTagsJson,
                                   String newCategory, String newTagsJson);
}
