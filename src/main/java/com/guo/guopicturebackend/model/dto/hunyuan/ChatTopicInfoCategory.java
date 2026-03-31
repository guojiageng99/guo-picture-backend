package com.guo.guopicturebackend.model.dto.hunyuan;

import lombok.Data;

import java.util.List;

/**
 * 混元返回的图话题信息（与教程 JSON 结构一致，供 Hutool JSONUtil.toBean 反序列化）
 */
@Data
public class ChatTopicInfoCategory {

    private String introduction;

    private List<String> tags;

    private String category;
}
