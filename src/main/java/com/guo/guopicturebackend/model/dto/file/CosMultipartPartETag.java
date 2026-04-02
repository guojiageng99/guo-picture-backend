package com.guo.guopicturebackend.model.dto.file;

import lombok.Data;

@Data
public class CosMultipartPartETag {

    private Integer partNumber;

    private String eTag;
}
