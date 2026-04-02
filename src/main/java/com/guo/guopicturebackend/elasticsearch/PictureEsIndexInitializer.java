package com.guo.guopicturebackend.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "elasticsearch", name = "enabled", havingValue = "true")
public class PictureEsIndexInitializer {

    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    @PostConstruct
    public void ensureIndex() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(PictureEsDocument.class);
            if (!indexOps.exists()) {
                indexOps.create();
                indexOps.putMapping(indexOps.createMapping(PictureEsDocument.class));
                log.info("Elasticsearch 索引已创建: guo_picture");
            }
        } catch (Exception e) {
            log.warn("Elasticsearch 暂不可用，跳过索引初始化（请检查服务器 ES 是否启动、本地 SSH 隧道是否包含 9200）: {}", e.getMessage());
        }
    }
}
