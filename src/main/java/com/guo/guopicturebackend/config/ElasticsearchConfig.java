package com.guo.guopicturebackend.config;

import com.guo.guopicturebackend.elasticsearch.PictureEsRepository;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "elasticsearch", name = "enabled", havingValue = "true")
@EnableElasticsearchRepositories(basePackageClasses = PictureEsRepository.class)
public class ElasticsearchConfig {

    @Bean
    public RestHighLevelClient elasticsearchClient(ElasticsearchProperties elasticsearchProperties) {
        // connectedTo 需要「host:port」，不能写 http://...，否则会把整串当主机名解析报 UnknownHostException
        String[] hosts = toHostPortEndpoints(elasticsearchProperties.getUris());
        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder = ClientConfiguration.builder()
                .connectedTo(hosts);
        if (elasticsearchProperties.getUris().stream().anyMatch(u -> u != null && u.trim().startsWith("https://"))) {
            builder.usingSsl();
        }
        return RestClients.create(builder.build()).rest();
    }

    private static String[] toHostPortEndpoints(List<String> uris) {
        List<String> out = new ArrayList<>();
        for (String u : uris) {
            if (u == null || u.isBlank()) {
                continue;
            }
            String s = u.trim();
            if (s.startsWith("https://")) {
                s = s.substring(8);
            } else if (s.startsWith("http://")) {
                s = s.substring(7);
            }
            while (s.endsWith("/")) {
                s = s.substring(0, s.length() - 1);
            }
            if (!s.contains(":")) {
                s = s + ":9200";
            }
            out.add(s);
        }
        if (out.isEmpty()) {
            out.add("127.0.0.1:9200");
        }
        return out.toArray(new String[0]);
    }

    /**
     * Repository 层默认按 Bean 名 {@code elasticsearchTemplate} 注入，需与 {@code elasticsearchOperations} 同名实例。
     */
    @Bean(name = {"elasticsearchTemplate", "elasticsearchOperations"})
    public ElasticsearchOperations elasticsearchOperations(RestHighLevelClient elasticsearchClient) {
        return new ElasticsearchRestTemplate(elasticsearchClient);
    }
}
