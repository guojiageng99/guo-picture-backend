package com.guo.guopicturebackend.api.imagesearch;

import com.guo.guopicturebackend.api.imagesearch.model.ImageSearchResult;
import com.guo.guopicturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.guo.guopicturebackend.api.imagesearch.sub.GetImageListApi;
import com.guo.guopicturebackend.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://img0.baidu.com/it/u=3032038457,1728181140&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500";
        List<ImageSearchResult> resultList = searchImage(imageUrl);
        System.out.println("结果列表" + resultList);
    }



}
