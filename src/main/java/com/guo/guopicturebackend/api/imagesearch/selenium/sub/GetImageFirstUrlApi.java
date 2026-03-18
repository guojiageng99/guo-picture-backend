package com.guo.guopicturebackend.api.imagesearch.selenium.sub;

import com.guo.guopicturebackend.api.imagesearch.selenium.model.ImageSearchResult;
import com.guo.guopicturebackend.exception.BusinessException;
import com.guo.guopicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class GetImageFirstUrlApi {

    // 最多返回10张相似图
    private static final int MAX_COUNT = 10;

    public List<ImageSearchResult> getImageFirstUrlList(String baiduHtml) {
        if (baiduHtml == null || baiduHtml.isBlank()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "百度HTML为空");
        }

        try {
            // 1. 解析HTML
            Document doc = Jsoup.parse(baiduHtml);
            // 2. 选择所有相似图的img标签（百度PC页面固定class）
            Elements imgElements = doc.select("ul.imgList li img");

            Set<String> urlSet = new HashSet<>();
            List<ImageSearchResult> resultList = new ArrayList<>();

            // 3. 提取图片URL
            for (Element img : imgElements) {
                if (resultList.size() >= MAX_COUNT) break;

                // 优先取data-src（懒加载URL），再取src
                String imgUrl = img.attr("data-src");
                if (imgUrl.isBlank()) {
                    imgUrl = img.attr("src");
                }

                // 过滤无效URL
                if (imgUrl.startsWith("http") && !urlSet.contains(imgUrl)) {
                    urlSet.add(imgUrl);
                    resultList.add(new ImageSearchResult(imgUrl));
                }
            }

            if (resultList.isEmpty()) {
                log.error("未找到相似图片，HTML关键部分：{}", baiduHtml.substring(0, Math.min(1000, baiduHtml.length())));
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到相似图片");
            }

            log.info("成功提取 {} 张相似图片", resultList.size());
            return resultList;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析HTML异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "解析图片失败：" + e.getMessage());
        }
    }
}