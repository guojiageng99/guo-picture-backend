package com.guo.guopicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.guo.guopicturebackend.mapper")
public class GuoPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuoPictureBackendApplication.class, args);
    }

}
