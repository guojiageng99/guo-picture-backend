package com.guo.guopicturebackend.model.dto.picture;

import com.guo.guopicturebackend.api.aliyun.model.CreateOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 空间 id，分表时必须传递。公共图库传 0
     */
    private Long spaceId;

    /**
     * 扩图参数
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;

    private static final long serialVersionUID = 1L;
}
