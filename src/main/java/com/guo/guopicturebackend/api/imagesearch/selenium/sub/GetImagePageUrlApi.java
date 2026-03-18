package com.guo.guopicturebackend.api.imagesearch.selenium.sub;

import com.guo.guopicturebackend.api.imagesearch.selenium.model.ImageSearchResult;
import com.guo.guopicturebackend.exception.BusinessException;
import com.guo.guopicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 百度识图 - 以图搜图（全新实现）
 * 支持：1) URL 粘贴  2) 本地上传（用于 COS 防盗链）
 */
@Slf4j
public class GetImagePageUrlApi {

    private static final String CHROME_DRIVER_PATH = "./chromedriver.exe";
    private static final String BAIDU_GRAPH_URL = "https://graph.baidu.com/pcpage/index?tpl_from=pc";
    private static final int MAX_RESULTS = 10;
    private static final int PAGE_LOAD_TIMEOUT = 45;
    private static final int ELEMENT_WAIT_TIMEOUT = 20;

    public List<ImageSearchResult> searchImage(String imageUrl) {
        return doSearch(imageUrl, null);
    }

    public List<ImageSearchResult> searchImageByFile(File file) {
        if (file == null || !file.exists()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不存在");
        }
        return doSearch(null, file.getAbsolutePath());
    }

    private List<ImageSearchResult> doSearch(String imageUrl, String filePath) {
        WebDriver driver = null;
        try {
            // 使用 JDK 内置 HTTP 客户端，避免 Netty 的 Origin 头导致 WebSocket 403
            System.setProperty("webdriver.http.factory", "jdk-http-client");
            System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);
            driver = new ChromeDriver(buildChromeOptions());
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(PAGE_LOAD_TIMEOUT));
            driver.manage().window().maximize();

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 1. 打开百度识图
            driver.get(BAIDU_GRAPH_URL);
            sleep(2000);
            closePopup(driver, js);

            // 2. 上传图片：文件 或 URL
            if (filePath != null) {
                uploadByFile(driver, wait, filePath);
            } else {
                uploadByUrl(driver, wait, js, imageUrl);
            }

            // 3. 等待结果页
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("div[class*='similar']")),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("img[src*='baidu.com']"))
            ));
            sleep(2000);
            closePopup(driver, js);

            // 4. 滚动加载
            for (int i = 0; i < 2; i++) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                sleep(2000);
            }
            js.executeScript("window.scrollTo(0, 0);");
            sleep(1000);

            // 5. 提取相似图 URL（优先从页面 JSON 提取 objurl，其次从 img 元素）
            List<ImageSearchResult> results = extractFromPageSource(driver.getPageSource());
            if (results.isEmpty()) {
                results = extractFromImgElements(driver);
            }

            if (results.isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未解析到相似图片");
            }

            log.info("以图搜图成功，提取 {} 张相似图", results.size());
            return results;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("以图搜图异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "以图搜图失败：" + e.getMessage());
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    log.warn("关闭浏览器失败: {}", e.getMessage());
                }
            }
        }
    }

    private void uploadByFile(WebDriver driver, WebDriverWait wait, String filePath) {
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='file']")));
        fileInput.sendKeys(filePath);
        log.info("本地上传完成");
        sleep(4000);
    }

    private void uploadByUrl(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String imageUrl) {
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input[placeholder*='粘贴'], input.graph-d20-search-wrapper-input, input[placeholder*='网址']")));
        input.clear();
        input.sendKeys(imageUrl);
        js.executeScript("arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input', {bubbles:true}));", input, imageUrl);
        sleep(500);

        try {
            WebElement btn = driver.findElement(By.cssSelector("button[type='submit'], div[class*='search-btn'], button[class*='search']"));
            js.executeScript("arguments[0].click();", btn);
        } catch (Exception e) {
            input.sendKeys(Keys.ENTER);
        }
        log.info("URL 提交完成");
        sleep(3000);
    }

    /**
     * 从页面源码中提取 objurl（百度将相似图数据嵌入 JSON/JS）
     */
    private List<ImageSearchResult> extractFromPageSource(String html) {
        Set<String> seen = new LinkedHashSet<>();
        List<ImageSearchResult> list = new ArrayList<>();

        // 多种字段名：objurl, objURL, thumburl, thumbURL
        Pattern[] patterns = {
                Pattern.compile("\"objurl\"\\s*:\\s*\"(https?://[^\"]+)\""),
                Pattern.compile("\"objURL\"\\s*:\\s*\"(https?://[^\"]+)\""),
                Pattern.compile("\"thumburl\"\\s*:\\s*\"(https?://[^\"]+)\""),
                Pattern.compile("\"thumbURL\"\\s*:\\s*\"(https?://[^\"]+)\""),
                Pattern.compile("objurl[\"']?\\s*:\\s*[\"'](https?://[^\"']+)[\"']")
        };

        for (Pattern p : patterns) {
            Matcher m = p.matcher(html);
            while (m.find() && list.size() < MAX_RESULTS) {
                String url = m.group(1).replace("\\/", "/").replace("\\u0026", "&");
                if (isValidImageUrl(url) && seen.add(url)) {
                    list.add(new ImageSearchResult(url));
                }
            }
            if (!list.isEmpty()) break;
        }

        if (!list.isEmpty()) {
            log.info("从页面 JSON 提取 objurl，数量: {}", list.size());
        }
        return list;
    }

    /**
     * 从 img 元素提取 URL（兜底方案）
     */
    private List<ImageSearchResult> extractFromImgElements(WebDriver driver) {
        Set<String> seen = new LinkedHashSet<>();
        List<ImageSearchResult> list = new ArrayList<>();

        String[] selectors = {
                "div[class*='imgcol'] img, div[class*='similar'] img, a[class*='imglink'] img",
                "img[src*='baidu.com'], img[data-src*='baidu.com'], img[src*='bdstatic.com']"
        };
        for (String selector : selectors) {
            try {
                List<WebElement> imgs = driver.findElements(By.cssSelector(selector));
                for (WebElement img : imgs) {
                    if (list.size() >= MAX_RESULTS) break;
                    String url = img.getAttribute("data-src");
                    if (url == null || url.isEmpty()) url = img.getAttribute("src");
                    if (url != null && !url.isEmpty()) {
                        url = url.replace("\\", "").trim();
                        if (isValidImageUrl(url) && seen.add(url)) {
                            list.add(new ImageSearchResult(url));
                        }
                    }
                    // 不提取父级 a 的 href，多为页面链接而非直接图片 URL
                }
                if (!list.isEmpty()) {
                    log.info("从 img 元素提取，数量: {}", list.size());
                    break;
                }
            } catch (Exception e) {
                log.debug("选择器 {} 失败: {}", selector, e.getMessage());
            }
        }
        return list;
    }

    /**
     * 校验是否为有效图片 URL（放宽规则以适配百度各种格式）
     */
    private boolean isValidImageUrl(String url) {
        if (url == null || url.length() < 15) return false;
        if (url.startsWith("data:") || url.contains("javascript:")) return false;
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false;

        String lower = url.toLowerCase();
        // 排除 logo、icon、favicon、头像、页面链接
        if (lower.contains("/logo") || lower.contains("logo.") || lower.contains("icon.") ||
                lower.contains("favicon") || lower.contains("avatar") ||
                lower.contains("/s?") || lower.contains("/link?") || lower.contains(".html")) return false;

        // 百度图床：it/u、bdstatic、graph/resource
        if (lower.contains("baidu.com") && (lower.contains("it/u") || lower.contains("it%2fu") ||
                lower.contains("bdstatic.com") || lower.contains("graph.baidu.com/resource"))) return true;
        if (lower.contains("bdstatic.com")) return true;
        // 扩展名或图片参数
        if (lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".png") ||
                lower.contains(".webp") || lower.contains(".gif") || lower.contains("f=jpeg") ||
                lower.contains("f=png") || lower.contains("fmt=") || lower.contains("fm=")) return true;
        // 其他图床域名
        if (lower.contains("alicdn.com") || lower.contains("sinaimg.cn") || lower.contains("qq.com")) return true;
        return false;
    }

    private void closePopup(WebDriver driver, JavascriptExecutor js) {
        try {
            String[] selectors = {
                    "div[class*='guide-info-btn'], .guide-close-btn, button[aria-label='关闭']",
                    "[class*='mask'] [class*='close'], .popup-close"
            };
            for (String sel : selectors) {
                try {
                    List<WebElement> btns = driver.findElements(By.cssSelector(sel));
                    for (WebElement b : btns) {
                        if (b.isDisplayed()) {
                            js.executeScript("arguments[0].click();", b);
                            sleep(300);
                            return;
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.debug("关闭弹窗: {}", e.getMessage());
        }
    }

    private ChromeOptions buildChromeOptions() {
        ChromeOptions opt = new ChromeOptions();
        opt.addArguments("--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
        opt.addArguments("--remote-allow-origins=*");
        opt.addArguments("--disable-blink-features=AutomationControlled");
        opt.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        opt.addArguments("--window-size=1920,1080");
        opt.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        opt.setExperimentalOption("useAutomationExtension", false);
        return opt;
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
