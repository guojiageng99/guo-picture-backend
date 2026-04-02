package com.guo.guopicturebackend.model.dto.hunyuan;

import lombok.Data;

/**
 * 混元图片内容初审结果（JSON：pass、reason）
 */
@Data
public class PictureModerationResult {

    private Boolean pass;

    private String reason;
}
