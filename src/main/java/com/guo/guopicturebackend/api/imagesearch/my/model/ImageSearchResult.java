package com.guo.guopicturebackend.api.imagesearch.my.model;

import lombok.Data;

/**
 * 相似图片结果封装（my 实现）
 * 前端需要 thumbUrl（缩略图）、fromUrl（原图链接）
 */
@Data
public class ImageSearchResult {
    /** 图片 URL（兼容旧字段） */
    private String url;
    /** 缩略图 URL，前端用于展示 */
    private String thumbUrl;
    /** 原图/来源链接，前端用于跳转 */
    private String fromUrl;

    public ImageSearchResult() {
    }

    public ImageSearchResult(String url) {
        this.url = url;
        this.thumbUrl = url;
        this.fromUrl = url;
    }
}
