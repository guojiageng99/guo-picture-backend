package com.guo.guopicturebackend.api.imagesearch.my;

import com.guo.guopicturebackend.api.imagesearch.my.model.ImageSearchResult;
import com.guo.guopicturebackend.exception.BusinessException;
import com.guo.guopicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 以图搜图门面类（my 实现）
 * COS 等防盗链图床：先下载到本地，再通过 Selenium 本地上传
 * 其他 URL：直接使用 Selenium 粘贴 URL
 */
@Service("myImageSearchApiFacade")
@Slf4j
public class ImageSearchApiFacade {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36";

    @Resource(name = "seleniumImageSearchApiFacade")
    private com.guo.guopicturebackend.api.imagesearch.selenium.ImageSearchApiFacade seleniumImageSearchFacade;

    @Resource
    private RestTemplate restTemplate;

    /**
     * 对外提供的以图搜图方法
     */
    public List<ImageSearchResult> searchImage(String imageUrl) {
        try {
            if (isCosUrl(imageUrl)) {
                log.info("检测到 COS 图床，使用本地上传模式");
                File tempFile = downloadToTempFile(imageUrl);
                try {
                    List<com.guo.guopicturebackend.api.imagesearch.selenium.model.ImageSearchResult> seleniumResults =
                            seleniumImageSearchFacade.searchImageByFile(tempFile);
                    return seleniumResults.stream()
                            .map(r -> new ImageSearchResult(r.getUrl()))
                            .collect(Collectors.toList());
                } finally {
                    if (tempFile != null && tempFile.exists()) {
                        tempFile.delete();
                    }
                }
            } else {
                List<com.guo.guopicturebackend.api.imagesearch.selenium.model.ImageSearchResult> seleniumResults =
                        seleniumImageSearchFacade.searchImage(imageUrl);
                return seleniumResults.stream()
                        .map(r -> new ImageSearchResult(r.getUrl()))
                        .collect(Collectors.toList());
            }
        } catch (BusinessException e) {
            log.error("以图搜图失败，imageUrl={}，原因：{}", imageUrl, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("以图搜图未知异常，imageUrl={}", imageUrl, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "以图搜图失败：" + e.getMessage());
        }
    }

    private boolean isCosUrl(String url) {
        return url != null && (url.contains("myqcloud.com") || url.contains("cos."));
    }

    private File downloadToTempFile(String imageUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> resp = restTemplate.exchange(imageUrl, HttpMethod.GET, entity, byte[].class);
            if (resp == null || resp.getBody() == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "无法下载图片");
            }
            String ext = imageUrl.toLowerCase().contains(".webp") ? ".webp" : imageUrl.toLowerCase().contains(".png") ? ".png" : ".jpg";
            File tempFile = File.createTempFile("img_search_", ext);
            Files.write(tempFile.toPath(), resp.getBody());
            return tempFile;
        } catch (Exception e) {
            log.error("下载图片失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "无法下载图片：" + e.getMessage());
        }
    }
}
