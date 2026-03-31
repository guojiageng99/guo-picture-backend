package com.guo.guopicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guo.guopicturebackend.exception.BusinessException;
import com.guo.guopicturebackend.exception.ErrorCode;
import com.guo.guopicturebackend.mapper.PictureTagMapper;
import com.guo.guopicturebackend.model.entity.PictureTag;
import com.guo.guopicturebackend.service.PictureTagService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PictureTagServiceImpl extends ServiceImpl<PictureTagMapper, PictureTag>
        implements PictureTagService {

    @Override
    public List<String> listAllTagNamesForPicker() {
        return this.lambdaQuery()
                .orderByAsc(PictureTag::getSortOrder)
                .orderByDesc(PictureTag::getUsageCount)
                .list()
                .stream()
                .map(PictureTag::getTagName)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listPopularTagNames(int limit) {
        return this.lambdaQuery()
                .orderByDesc(PictureTag::getUsageCount)
                .orderByAsc(PictureTag::getSortOrder)
                .last("LIMIT " + limit)
                .list()
                .stream()
                .map(PictureTag::getTagName)
                .collect(Collectors.toList());
    }

    @Override
    public void incrementUsageIfRegistered(String tagName) {
        if (StrUtil.isBlank(tagName)) {
            return;
        }
        this.lambdaUpdate()
                .eq(PictureTag::getTagName, tagName.trim())
                .setSql("usage_count = usage_count + 1")
                .update();
    }

    @Override
    public void decrementUsageIfRegistered(String tagName) {
        if (StrUtil.isBlank(tagName)) {
            return;
        }
        this.lambdaUpdate()
                .eq(PictureTag::getTagName, tagName.trim())
                .gt(PictureTag::getUsageCount, 0)
                .setSql("usage_count = usage_count - 1")
                .update();
    }

    @Override
    public Page<PictureTag> listPage(long current, long pageSize) {
        return this.page(new Page<>(current, pageSize),
                new QueryWrapper<PictureTag>().orderByAsc("sort_order").orderByDesc("usage_count"));
    }

    @Override
    public long addTag(String tagName, Integer sortOrder) {
        if (StrUtil.isBlank(tagName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签名不能为空");
        }
        String name = tagName.trim();
        long c = this.lambdaQuery().eq(PictureTag::getTagName, name).count();
        if (c > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签已存在");
        }
        PictureTag tag = new PictureTag();
        tag.setTagName(name);
        tag.setUsageCount(0);
        tag.setSortOrder(sortOrder != null ? sortOrder : 0);
        boolean ok = this.save(tag);
        if (!ok) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return tag.getId();
    }

    @Override
    public void updateTag(Long id, String newTagName, Integer sortOrder) {
        PictureTag old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (StrUtil.isNotBlank(newTagName) && !newTagName.trim().equals(old.getTagName())) {
            if (old.getUsageCount() != null && old.getUsageCount() > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该标签已被图片使用，不能修改名称");
            }
            String name = newTagName.trim();
            long dup = this.lambdaQuery().eq(PictureTag::getTagName, name).ne(PictureTag::getId, id).count();
            if (dup > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签名已存在");
            }
            old.setTagName(name);
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
    public void deleteTag(Long id) {
        PictureTag tag = this.getById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (tag.getUsageCount() != null && tag.getUsageCount() > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该标签仍被图片引用，无法删除");
        }
        boolean ok = this.removeById(id);
        if (!ok) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }
}
