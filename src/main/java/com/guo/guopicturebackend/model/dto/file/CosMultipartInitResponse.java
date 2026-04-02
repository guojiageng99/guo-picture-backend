package com.guo.guopicturebackend.model.dto.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分片上传初始化结果（供前端按 uploadId + key 续传分块）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CosMultipartInitResponse {

    private String uploadId;

    /** COS 对象键（含前导 /） */
    private String key;
}
