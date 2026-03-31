package com.guo.guopicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureDictionaryPageRequest implements Serializable {
    private long current = 1;
    private long pageSize = 10;
    private static final long serialVersionUID = 1L;
}
