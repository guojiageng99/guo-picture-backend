package com.guo.guopicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guo.guopicturebackend.model.entity.PictureTag;

import java.util.List;

public interface PictureTagService extends IService<PictureTag> {

    List<String> listAllTagNamesForPicker();

    List<String> listPopularTagNames(int limit);

    void incrementUsageIfRegistered(String tagName);

    void decrementUsageIfRegistered(String tagName);

    Page<PictureTag> listPage(long current, long pageSize);

    long addTag(String tagName, Integer sortOrder);

    void updateTag(Long id, String newTagName, Integer sortOrder);

    void deleteTag(Long id);
}
