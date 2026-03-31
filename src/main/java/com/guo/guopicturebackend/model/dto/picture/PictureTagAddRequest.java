package com.guo.guopicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureTagAddRequest implements Serializable {
    private String tagName;
    private Integer sortOrder;
    private static final long serialVersionUID = 1L;
}
