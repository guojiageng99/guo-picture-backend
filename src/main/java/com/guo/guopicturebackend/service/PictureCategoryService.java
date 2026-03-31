package com.guo.guopicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guo.guopicturebackend.model.entity.PictureCategory;

import java.util.List;

public interface PictureCategoryService extends IService<PictureCategory> {

    List<String> listAllCategoryNamesForPicker();

    List<String> listPopularCategoryNames(int limit);

    void incrementUsageIfRegistered(String categoryName);

    void decrementUsageIfRegistered(String categoryName);

    Page<PictureCategory> listPage(long current, long pageSize);

    long addCategory(String categoryName, Integer sortOrder);

    void updateCategory(Long id, String newCategoryName, Integer sortOrder);

    void deleteCategory(Long id);
}
