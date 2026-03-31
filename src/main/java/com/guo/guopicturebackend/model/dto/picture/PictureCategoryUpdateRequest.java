package com.guo.guopicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureCategoryUpdateRequest implements Serializable {
    private Long id;
    private String categoryName;
    private Integer sortOrder;
    private static final long serialVersionUID = 1L;
}
