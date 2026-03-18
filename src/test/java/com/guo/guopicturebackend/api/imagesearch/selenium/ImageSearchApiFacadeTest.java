package com.guo.guopicturebackend.api.imagesearch.selenium;

import com.guo.guopicturebackend.api.imagesearch.selenium.model.ImageSearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
public class ImageSearchApiFacadeTest {

    @Resource
    private ImageSearchApiFacade imageSearchApiFacade;

    @Test
    public void testSearchImage() {
        String testImageUrl = "https://img0.baidu.com/it/u=3032038457,1728181140&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500";
        try {
            List<ImageSearchResult> resultList = imageSearchApiFacade.searchImage(testImageUrl);
            System.out.println("===== 测试成功 =====");
            System.out.println("相似图片数量：" + resultList.size());
            for (int i = 0; i < resultList.size(); i++) {
                System.out.println("第" + (i+1) + "张：" + resultList.get(i).getUrl());
            }
        } catch (Exception e) {
            System.err.println("测试失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}