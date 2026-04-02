package com.guo.guopicturebackend.model.dto.file;

import lombok.Data;

import java.util.List;

@Data
public class CosMultipartCompleteRequest {

    private String uploadId;

    private String key;

    private List<CosMultipartPartETag> parts;
}
