package com.guo.guopicturebackend.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PictureEsRepository extends ElasticsearchRepository<PictureEsDocument, String> {
}
