package com.guo.guopicturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/**
 * 可选全文检索：启用后需本机或集群可访问的 Elasticsearch 7.x
 */
@Data
@ConfigurationProperties(prefix = "elasticsearch")
public class ElasticsearchProperties {

    private boolean enabled = false;

    /** 索引名 */
    private String indexName = "guo_picture";

    /** 连接地址，如 http://127.0.0.1:9200 */
    private List<String> uris = Collections.singletonList("http://127.0.0.1:9200");
}
