package com.guo.guopicturebackend.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * 图片全文检索文档（名称 + 简介，CJK 二元分词，无需 IK 插件）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "guo_picture")
public class PictureEsDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long spaceId;

    @Field(type = FieldType.Text, analyzer = "cjk", searchAnalyzer = "cjk")
    private String name;

    @Field(type = FieldType.Text, analyzer = "cjk", searchAnalyzer = "cjk")
    private String introduction;

    @Field(type = FieldType.Integer)
    private Integer reviewStatus;

    @Field(type = FieldType.Keyword)
    private String category;
}
