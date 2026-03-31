package com.guo.guopicturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 腾讯混元（图片理解等）配置
 */
@Data
@ConfigurationProperties(prefix = "tencent.hunyuan")
public class HunyuanProperties {

    /** 是否启用混元 API */
    private boolean enabled = false;

    private String secretId = "";

    private String secretKey = "";

    /** 地域，如 ap-guangzhou */
    private String region = "ap-guangzhou";

    /** 多模态看图模型，如 hunyuan-vision */
    private String model = "hunyuan-vision";

    /** 新图上传成功后是否异步写入 AI 生成的简介/标签/分类 */
    private boolean autoFillAfterUpload = false;
}
