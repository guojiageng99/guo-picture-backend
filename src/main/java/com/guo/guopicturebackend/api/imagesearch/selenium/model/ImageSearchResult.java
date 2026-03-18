package com.guo.guopicturebackend.api.imagesearch.selenium.model;

import lombok.Data;

/**
 * 相似图片结果封装
 */
@Data
public class ImageSearchResult {
    /**
     * 相似图片 URL
     */
    private String url;

    public ImageSearchResult(String url) {
        this.url = url;
    }
}