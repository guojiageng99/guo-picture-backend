package com.guo.guopicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guo.guopicturebackend.exception.BusinessException;
import com.guo.guopicturebackend.exception.ErrorCode;
import com.guo.guopicturebackend.mapper.PictureCategoryMapper;
import com.guo.guopicturebackend.model.entity.PictureCategory;
import com.guo.guopicturebackend.service.PictureCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PictureCategoryServiceImpl extends ServiceImpl<PictureCategoryMapper, PictureCategory>
        implements PictureCategoryService {

    @Override
    public List<String> listAllCategoryNamesForPicker() {
        return this.lambdaQuery()
                .orderByAsc(PictureCategory::getSortOrder)
                .orderByDesc(PictureCategory::getUsageCount)
                .list()
                .stream()
                .map(PictureCategory::getCategoryName)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listPopularCategoryNames(int limit) {
        return this.lambdaQuery()
                .orderByDesc(PictureCategory::getUsageCount)
                .orderByAsc(PictureCategory::getSortOrder)
                .last("LIMIT " + limit)
                .list()
                .stream()
                .map(PictureCategory::getCategoryName)
                .collect(Collectors.toList());
    }

    @Override
    public void incrementUsageIfRegistered(String categoryName) {
        if (StrUtil.isBlank(categoryName)) {
            return;
        }
        this.lambdaUpdate()
                .eq(PictureCategory::getCategoryName, categoryName.trim())
                .setSql("usage_count = usage_count + 1")
                .update();
    }

    @Override
    public void decrementUsageIfRegistered(String categoryName) {
        if (StrUtil.isBlank(categoryName)) {
            return;
        }
        this.lambdaUpdate()
                .eq(PictureCategory::getCategoryName, categoryName.trim())
                .gt(PictureCategory::getUsageCount, 0)
                .setSql("usage_count = usage_count - 1")
                .update();
    }

    @Override
    public Page<PictureCategory> listPage(long current, long pageSize) {
        return this.page(new Page<>(current, pageSize),
                new QueryWrapper<PictureCategory>().orderByAsc("sort_order").orderByDesc("usage_count"));
    }

    @Override
    public long addCategory(String categoryName, Integer sortOrder) {
        if (StrUtil.isBlank(categoryName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类名不能为空");
        }
        String name = categoryName.trim();
        long c = this.lambdaQuery().eq(PictureCategory::getCategoryName, name).count();
        if (c > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类已存在");
        }
        PictureCategory cat = new PictureCategory();
        cat.setCategoryName(name);
        cat.setUsageCount(0);
        cat.setSortOrder(sortOrder != null ? sortOrder : 0);
        boolean ok = this.save(cat);
        if (!ok) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return cat.getId();
    }

    @Override
    public void updateCategory(Long id, String newCategoryName, Integer sortOrder) {
        PictureCategory old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (StrUtil.isNotBlank(newCategoryName) && !newCategoryName.trim().equals(old.getCategoryName())) {
            if (old.getUsageCount() != null && old.getUsageCount() > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该分类已被图片使用，不能修改名称");
            }
            String name = newCategoryName.trim();
            long dup = this.lambdaQuery().eq(PictureCategory::getCategoryName, name).ne(PictureCategory::getId, id).count();
            if (dup > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类名已存在");
            }
            old.setCategoryName(name);
        }
        if (sortOrder != null) {
            old.setSortOrder(sortOrder);
        }
        boolean ok = this.updateById(old);
        if (!ok) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    @Override
    public void deleteCategory(Long id) {
        PictureCategory cat = this.getById(id);
        if (cat == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (cat.getUsageCount() != null && cat.getUsageCount() > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该分类仍被图片引用，无法删除");
        }
        boolean ok = this.removeById(id);
        if (!ok) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }
}
