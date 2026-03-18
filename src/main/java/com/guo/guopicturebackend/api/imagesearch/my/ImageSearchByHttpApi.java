package com.guo.guopicturebackend.api.imagesearch.my;

import com.guo.guopicturebackend.api.imagesearch.my.model.ImageSearchResult;
import com.guo.guopicturebackend.exception.BusinessException;
import com.guo.guopicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于 HTTP 的以图搜图实现（无需 Selenium/ChromeDriver）
 * 调用百度识图 graph.baidu.com 接口
 */
@Slf4j
@Component
public class ImageSearchByHttpApi {

    private static final String UPLOAD_URL = "https://graph.baidu.com/upload";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int MAX_IMAGE_COUNT = 10;
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;

    @Resource
    private RestTemplate restTemplate;

    /**
     * 以图搜图：根据图片 URL 搜索相似图片
     *
     * @param imageUrl 原始图片 URL
     * @return 相似图片列表
     */
    public List<ImageSearchResult> searchImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片URL不能为空");
        }

        try {
            // 1. 调用百度识图上传接口
            // 腾讯云 COS 等图床有防盗链，百度无法拉取会返回 Reject，直接使用二进制上传
            // 其他 URL 先尝试传 URL，失败再回退到二进制上传
            String resultPageUrl = null;
            if (imageUrl.contains("myqcloud.com") || imageUrl.contains("cos.")) {
                log.info("检测到 COS 图床，使用二进制上传避免 Reject");
                resultPageUrl = uploadImageBytes(imageUrl);
            } else {
                try {
                    resultPageUrl = uploadImageUrl(imageUrl);
                } catch (BusinessException e) {
                    if (e.getMessage() != null && e.getMessage().contains("Reject")) {
                        log.info("URL 上传被拒，尝试下载图片后以二进制上传");
                        resultPageUrl = uploadImageBytes(imageUrl);
                    } else {
                        throw e;
                    }
                }
            }
            if (resultPageUrl == null || resultPageUrl.isBlank()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "百度识图接口未返回结果页地址");
            }

            log.info("以图搜图：获取到结果页URL={}", resultPageUrl);

            // 2. 请求结果页并解析相似图
            List<ImageSearchResult> resultList = parseSimilarImages(resultPageUrl);

            if (resultList.isEmpty()) {
                // 尝试从页面中提取 objurl（百度结果页可能将图片URL放在 data 属性或 script 中）
                resultList = parseSimilarImagesFromScript(resultPageUrl);
            }

            if (resultList.isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未解析到相似图片，页面结构可能已更新");
            }

            log.info("以图搜图成功，返回{}张相似图", resultList.size());
            return resultList;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("以图搜图失败，imageUrl={}", imageUrl, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "以图搜图失败：" + e.getMessage());
        }
    }

    /**
     * 上传图片URL到百度识图，获取结果页地址
     */
    private String uploadImageUrl(String imageUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", USER_AGENT);
        headers.set("Referer", "https://graph.baidu.com/");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("from", "pc");
        params.add("image", imageUrl);
        params.add("tn", "pc");
        params.add("image_source", "PC_UPLOAD_SEARCH_FILE");
        params.add("range", "{\"page_from\": \"searchResult\"}");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                UPLOAD_URL,
                HttpMethod.POST,
                request,
                Map.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "百度识图接口请求失败");
        }

        Map<String, Object> body = response.getBody();
        Object status = body.get("status");
        if (status == null || !Integer.valueOf(0).equals(status)) {
            String msg = String.valueOf(body.getOrDefault("msg", "未知错误"));
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "百度识图接口返回错误：" + msg);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        if (data == null) {
            return null;
        }
        return (String) data.get("url");
    }

    /**
     * 下载图片后以二进制上传到百度识图（解决 COS 等防盗链导致 URL 被 Reject 的问题）
     */
    private String uploadImageBytes(String imageUrl) {
        byte[] imageBytes = downloadImage(imageUrl);
        if (imageBytes == null || imageBytes.length == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "无法下载图片，请检查图片URL是否可访问");
        }

        String filename = "image" + getExtensionFromUrl(imageUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("User-Agent", USER_AGENT);
        headers.set("Referer", "https://graph.baidu.com/");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("from", "pc");
        body.add("tn", "pc");
        body.add("image_source", "PC_UPLOAD_SEARCH_FILE");
        body.add("range", "{\"page_from\": \"searchResult\"}");
        body.add("image", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(UPLOAD_URL, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "百度识图接口请求失败");
        }

        Map<String, Object> respBody = response.getBody();
        Object status = respBody.get("status");
        if (status == null || !Integer.valueOf(0).equals(status)) {
            String msg = String.valueOf(respBody.getOrDefault("msg", "未知错误"));
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "百度识图接口返回错误：" + msg);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) respBody.get("data");
        if (data == null) return null;
        return (String) data.get("url");
    }

    private byte[] downloadImage(String imageUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Referer", "https://graph.baidu.com/");
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> resp = restTemplate.exchange(imageUrl, HttpMethod.GET, entity, byte[].class);
            return resp != null && resp.getBody() != null ? resp.getBody() : null;
        } catch (Exception e) {
            log.warn("下载图片失败: imageUrl={}, error={}", imageUrl, e.getMessage());
            return null;
        }
    }

    private String getExtensionFromUrl(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".webp")) return ".webp";
        if (lower.contains(".png")) return ".png";
        if (lower.contains(".gif")) return ".gif";
        return ".jpg";
    }

    /**
     * 解析结果页 HTML，提取相似图片 URL
     */
    private List<ImageSearchResult> parseSimilarImages(String resultPageUrl) {
        try {
            Document doc = Jsoup.connect(resultPageUrl)
                    .userAgent(USER_AGENT)
                    .referrer("https://graph.baidu.com/")
                    .timeout(READ_TIMEOUT_MS)
                    .ignoreContentType(true)
                    .get();

            Set<String> urlSet = new LinkedHashSet<>();
            List<ImageSearchResult> resultList = new ArrayList<>();

            // 多种选择器适配百度识图结果页结构
            String[] imgSelectors = {
                    "div.general-imgcol-item img[src], div.general-imgcol-item img[data-src]",
                    "div[class*='imgcol-item'] img[src], div[class*='imgcol-item'] img[data-src]",
                    "a[class*='imglink'] img[src], a[class*='imglink'] img[data-src]",
                    "a[class*='imglink']",
                    "img[src*='baidu.com'][src*='.jpg'], img[src*='baidu.com'][src*='.png']",
                    "img[data-src*='baidu.com']",
                    "img[src^='http']"
            };

            for (String selector : imgSelectors) {
                Elements elements = doc.select(selector);
                for (Element el : elements) {
                    if (resultList.size() >= MAX_IMAGE_COUNT) break;

                    String imgUrl = el.hasAttr("data-src") ? el.attr("data-src") : el.attr("src");
                    if (el.tagName().equals("a")) {
                        imgUrl = el.attr("href");
                    }
                    if (imgUrl == null) imgUrl = "";

                    imgUrl = imgUrl.replace("\\", "").trim();
                    if (isValidImageUrl(imgUrl) && !urlSet.contains(imgUrl)) {
                        urlSet.add(imgUrl);
                        resultList.add(new ImageSearchResult(imgUrl));
                    }
                }
                if (!resultList.isEmpty()) break;
            }

            return resultList;
        } catch (Exception e) {
            log.warn("解析结果页HTML失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 从页面 script 或 data 属性中解析 objurl（百度结果页可能将数据嵌入 JS）
     */
    private List<ImageSearchResult> parseSimilarImagesFromScript(String resultPageUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Referer", "https://graph.baidu.com/");
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(resultPageUrl, HttpMethod.GET, entity, String.class);
            String html = resp != null ? resp.getBody() : null;
            if (html == null || html.isEmpty()) return Collections.emptyList();

            Set<String> urlSet = new LinkedHashSet<>();
            List<ImageSearchResult> resultList = new ArrayList<>();

            // 匹配 objurl、thumbUrl 等图片URL（含 .jpg 或 img*.baidu.com 等）
            Pattern[] patterns = {
                    Pattern.compile("[\"'](https?://[^\"']+\\.(?:jpg|jpeg|png|webp|gif)(?:[?&#][^\"']*)?)[\"']"),
                    Pattern.compile("[\"'](https?://img[^\"']*baidu\\.com[^\"']*)[\"']")
            };
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(html);
                while (matcher.find() && resultList.size() < MAX_IMAGE_COUNT) {
                    String url = matcher.group(1).replace("\\/", "/");
                    if (isValidImageUrl(url) && !urlSet.contains(url)) {
                        urlSet.add(url);
                        resultList.add(new ImageSearchResult(url));
                    }
                }
                if (!resultList.isEmpty()) break;
            }

            return resultList;
        } catch (Exception e) {
            log.warn("从script解析相似图失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private boolean isValidImageUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        if (url.startsWith("data:image/")) return false;
        if (!url.startsWith("http")) return false;
        String lower = url.toLowerCase();
        // 扩展名或百度图床（如 img0.baidu.com/it/u=xxx&f=JPEG）
        return lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".png")
                || lower.contains(".webp") || lower.contains(".gif")
                || (lower.contains("baidu.com") && (lower.contains("img") || lower.contains("f=jpeg") || lower.contains("fmt=")));
    }
}
