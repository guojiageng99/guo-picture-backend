package com.guo.guopicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import com.guo.guopicturebackend.config.HunyuanProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties(HunyuanProperties.class)
@EnableAsync
@MapperScan("com.guo.guopicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class GuoPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuoPictureBackendApplication.class, args);
    }

}
