package com.guo.guopicturebackend.model.dto.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已上传分块信息（断点续传前查询）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CosMultipartListedPartVO {

    private int partNumber;

    private String eTag;

    private long size;

    private long lastModified;
}
