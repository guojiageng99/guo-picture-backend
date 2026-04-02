package com.guo.guopicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import com.guo.guopicturebackend.config.HunyuanProperties;
import com.guo.guopicturebackend.config.ElasticsearchProperties;
import com.guo.guopicturebackend.config.PictureCacheProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {
        ElasticsearchRestClientAutoConfiguration.class,
        ElasticsearchDataAutoConfiguration.class
})
@EnableConfigurationProperties({HunyuanProperties.class, PictureCacheProperties.class, ElasticsearchProperties.class})
@EnableAsync
@MapperScan("com.guo.guopicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class GuoPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuoPictureBackendApplication.class, args);
    }

}
