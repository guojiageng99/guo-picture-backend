package com.guo.guopicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureTagUpdateRequest implements Serializable {
    private Long id;
    private String tagName;
    private Integer sortOrder;
    private static final long serialVersionUID = 1L;
}
