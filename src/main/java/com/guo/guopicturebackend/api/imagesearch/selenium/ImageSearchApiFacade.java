package com.guo.guopicturebackend.api.imagesearch.selenium;

import com.guo.guopicturebackend.api.imagesearch.selenium.model.ImageSearchResult;
import com.guo.guopicturebackend.api.imagesearch.selenium.sub.GetImagePageUrlApi;
import com.guo.guopicturebackend.exception.BusinessException;
import com.guo.guopicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service("seleniumImageSearchApiFacade")
@Slf4j
public class ImageSearchApiFacade {

    private final GetImagePageUrlApi getImagePageUrlApi = new GetImagePageUrlApi();

    /**
     * 对外提供的以图搜图方法（URL 模式）
     */
    public List<ImageSearchResult> searchImage(String imageUrl) {
        try {
            return getImagePageUrlApi.searchImage(imageUrl);
        } catch (BusinessException e) {
            log.error("图片搜索失败，原始 URL：{}，原因：{}", imageUrl, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("图片搜索未知异常，原始 URL：{}", imageUrl, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "以图搜图失败：" + e.getMessage());
        }
    }

    /**
     * 通过本地文件上传以图搜图（适用于 COS 等防盗链图床）
     */
    public List<ImageSearchResult> searchImageByFile(File uploadFile) {
        try {
            return getImagePageUrlApi.searchImageByFile(uploadFile);
        } catch (BusinessException e) {
            log.error("文件上传搜图失败，文件：{}，原因：{}", uploadFile, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("文件上传搜图未知异常，文件：{}", uploadFile, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "以图搜图失败：" + e.getMessage());
        }
    }
}