package com.guo.guopicturebackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 图片标签分类列表视图
 */
@Data
public class PictureTagCategory {

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 分类列表
     */
    private List<String> categoryList;

    /**
     * 热门标签（按 usage_count）
     */
    private List<String> popularTagList;

    /**
     * 热门分类（按 usage_count）
     */
    private List<String> popularCategoryList;
}
