package com.guo.guopicturebackend.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 空间 id，分表时必须传递。公共图库传 0
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
