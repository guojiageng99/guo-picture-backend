package com.guo.guopicturebackend.model.dto.picture;

import com.guo.guopicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 扩图任务分页（管理员可查指定 userId）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PictureOutpaintTaskQueryRequest extends PageRequest {

    private Long userId;

    private static final long serialVersionUID = 1L;
}
