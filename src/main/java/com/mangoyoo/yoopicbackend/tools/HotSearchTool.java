package com.mangoyoo.yoopicbackend.tools;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.mangoyoo.yoopicbackend.manager.upload.OtherFileUpload;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 热点URL搜索工具（获取澎湃新闻热榜详细内容）
 */
@Slf4j
@Component
public class HotSearchTool {
    private static final String TARGET_URL = "https://tophub.today/n/5PdMxAbvmg";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

    @Resource
    private OtherFileUpload otherFileUpload;

    @Tool(description = """
    Get today's hot news detailed content from Internet.
    This tool fetches the top news links and extracts title, content, and images from each article.
    Returns formatted content with labeled title, main text, and image URLs for each article.
    """)
    public String getHotNewsContent(@ToolParam(description = "Number of top news articles to fetch detailed content (maximum 10)") Integer count, @ToolParam(description = "A summary of completed steps and explanation of the next steps in Chinese") String summary) {
        log.info(summary);
        try {
            // 参数验证
            if (count == null || count <= 0) {
                return "错误：count参数必须大于0";
            }
            if (count > 10) {
                count = 10; // 限制最大数量为10，避免请求过多
            }

            // 首先获取热榜链接和预览图片
            List<NewsLink> newsLinks = getHotNewsLinks(count);
            if (newsLinks.isEmpty()) {
                return "错误：未获取到热榜链接";
            }

            // 逐一访问每个链接获取详细内容
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < newsLinks.size(); i++) {
                NewsLink newsLink = newsLinks.get(i);
                NewsContent content = extractNewsContent(newsLink.url, newsLink.previewImageUrl);

                // 上传图片到第三方存储
                String uploadedImageUrl = uploadImageToStorage(content.imageUrl);
                if (uploadedImageUrl != null) {
                    content.imageUrl = uploadedImageUrl;
                }

                result.append("TOP").append(i + 1).append(":");
                result.append("标题=").append(content.title);
                result.append(",正文=").append(content.content);
                result.append(",图片=").append(content.imageUrl);
                if (i < newsLinks.size() - 1) {
                    result.append("\n");
                }
            }

            return result.toString();
        } catch (Exception e) {
            log.error("处理请求时发生异常", e);
            return "错误：处理请求时发生异常：" + e.getMessage();
        }
    }

    /**
     * 上传图片到第三方存储
     */
    private String uploadImageToStorage(String imageUrl) {
        if (StrUtil.isEmpty(imageUrl) || "无图片".equals(imageUrl)) {
            return null;
        }

        try {
            log.info("开始下载图片: {}", imageUrl);

            // 下载图片
            HttpResponse response = HttpRequest.get(imageUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "https://www.thepaper.cn/")
                    .timeout(15000)
                    .execute();

            if (!response.isOk()) {
                log.warn("图片下载失败，状态码: {}", response.getStatus());
                return null;
            }

            byte[] imageBytes = response.bodyBytes();
            if (imageBytes == null || imageBytes.length == 0) {
                log.warn("图片下载失败，内容为空");
                return null;
            }

            // 从URL获取文件扩展名
            String fileName = getFileNameFromUrl(imageUrl);
            String contentType = getContentTypeFromUrl(imageUrl);

            log.info("图片下载成功，文件大小: {} bytes，文件名: {}", imageBytes.length, fileName);

            // 创建MultipartFile
            MultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    fileName,
                    contentType,
                    imageBytes
            );

            // 上传到第三方存储
            log.info("开始上传图片到云存储");
            String uploadUrl = otherFileUpload.uploadFile(multipartFile, "images");

            log.info("图片上传成功，URL: {}", uploadUrl);
            return uploadUrl;

        } catch (Exception e) {
            log.error("上传图片失败: {}", imageUrl, e);
            return null;
        }
    }

    /**
     * 从URL获取文件名
     */
    private String getFileNameFromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            String path = url.getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);

            // 如果文件名为空或没有扩展名，生成一个默认名称
            if (StrUtil.isEmpty(fileName) || !fileName.contains(".")) {
                String extension = getFileExtensionFromUrl(imageUrl);
                fileName = "image_" + System.currentTimeMillis() + extension;
            }

            return fileName;
        } catch (Exception e) {
            // 如果解析失败，使用默认文件名
            return "image_" + System.currentTimeMillis() + ".jpg";
        }
    }

    /**
     * 从URL获取文件扩展名
     */
    private String getFileExtensionFromUrl(String imageUrl) {
        String lowerUrl = imageUrl.toLowerCase();
        if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg")) {
            return ".jpg";
        } else if (lowerUrl.contains(".png")) {
            return ".png";
        } else if (lowerUrl.contains(".gif")) {
            return ".gif";
        } else if (lowerUrl.contains(".webp")) {
            return ".webp";
        } else if (lowerUrl.contains(".bmp")) {
            return ".bmp";
        } else {
            return ".jpg"; // 默认扩展名
        }
    }

    /**
     * 从URL获取Content-Type
     */
    private String getContentTypeFromUrl(String imageUrl) {
        String extension = getFileExtensionFromUrl(imageUrl);
        switch (extension) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".webp":
                return "image/webp";
            case ".bmp":
                return "image/bmp";
            default:
                return "image/jpeg";
        }
    }

    /**
     * 获取热榜链接和预览图片列表
     */
    private List<NewsLink> getHotNewsLinks(Integer count) {
        try {
            HttpResponse response = HttpRequest.get(TARGET_URL)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", "https://tophub.today/")
                    .timeout(10000)
                    .execute();

            if (!response.isOk()) {
                return new ArrayList<>();
            }

            String html = response.body();
            if (StrUtil.isEmpty(html)) {
                return new ArrayList<>();
            }

            Document doc = Jsoup.parse(html);
            // 选择所有的新闻行
            Elements newsRows = doc.select("table.table tbody tr");

            List<NewsLink> newsLinks = new ArrayList<>();
            for (int i = 0; i < Math.min(count, newsRows.size()); i++) {
                Element row = newsRows.get(i);

                // 获取预览图片 - 从第二个td中的img标签
                Element previewImgElement = row.select("td.al img").first();
                String previewImageUrl = "";
                if (previewImgElement != null) {
                    previewImageUrl = previewImgElement.attr("src");
                    // 验证预览图片URL的有效性
                    if (!isValidImageUrl(previewImageUrl)) {
                        previewImageUrl = "";
                    }
                }

                // 获取新闻链接 - 从第三个td中的a标签
                Element linkElement = row.select("td.al div a[href]").first();
                if (linkElement != null) {
                    String href = linkElement.attr("href");
                    if (StrUtil.isNotEmpty(href) && (href.startsWith("http://") || href.startsWith("https://"))) {
                        NewsLink newsLink = new NewsLink();
                        newsLink.url = href;
                        newsLink.previewImageUrl = previewImageUrl;
                        newsLinks.add(newsLink);
                    }
                }
            }

            return newsLinks;
        } catch (Exception e) {
            log.error("获取热榜链接失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 提取单个新闻页面的内容
     */
    private NewsContent extractNewsContent(String url, String previewImageUrl) {
        NewsContent content = new NewsContent();
        try {
            HttpResponse response = HttpRequest.get(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", "https://tophub.today/")
                    .timeout(15000)
                    .execute();

            if (!response.isOk()) {
                content.title = "无法获取标题";
                content.content = "无法获取内容";
                content.imageUrl = StrUtil.isNotEmpty(previewImageUrl) ? previewImageUrl : "无图片";
                return content;
            }

            String html = response.body();
            if (StrUtil.isEmpty(html)) {
                content.title = "无法获取标题";
                content.content = "无法获取内容";
                content.imageUrl = StrUtil.isNotEmpty(previewImageUrl) ? previewImageUrl : "无图片";
                return content;
            }

            Document doc = Jsoup.parse(html);

            // 提取标题 - 根据提供的结构：<h1 class="index_title__B8mhI">
            Element titleElement = doc.selectFirst("h1.index_title__B8mhI");
            if (titleElement != null) {
                content.title = titleElement.text().trim();
            } else {
                // 备用选择器
                titleElement = doc.selectFirst("h1");
                if (titleElement != null) {
                    content.title = titleElement.text().trim();
                } else {
                    content.title = "无法获取标题";
                }
            }

            // 提取正文 - 根据提供的结构：<div class="index_cententWrap__Jv8jK">
            Element contentElement = doc.selectFirst("div.index_cententWrap__Jv8jK");
            if (contentElement != null) {
                // 获取所有段落文本
                Elements paragraphs = contentElement.select("p");
                StringBuilder contentText = new StringBuilder();
                for (Element p : paragraphs) {
                    String text = p.text().trim();
                    if (StrUtil.isNotEmpty(text)) {
                        contentText.append(text).append(" ");
                    }
                }
                content.content = contentText.toString().trim();
                if (StrUtil.isEmpty(content.content)) {
                    // 如果段落为空，获取整个div的文本
                    content.content = contentElement.text().trim();
                }
            } else {
                content.content = "无法获取正文内容";
            }

            // 提取图片 - 优化后的策略，如果没有找到则使用预览图片
            String imageUrl = extractImageUrl(doc);
            if ("无图片".equals(imageUrl) && StrUtil.isNotEmpty(previewImageUrl)) {
                // 如果没有找到图片，使用预览图片
                content.imageUrl = previewImageUrl;
            } else {
                content.imageUrl = imageUrl;
            }

        } catch (Exception e) {
            log.error("提取新闻内容失败: {}", url, e);
            content.title = "获取失败：" + e.getMessage();
            content.content = "获取失败";
            content.imageUrl = StrUtil.isNotEmpty(previewImageUrl) ? previewImageUrl : "无图片";
        }

        return content;
    }

    /**
     * 优化后的图片提取策略
     */
    private String extractImageUrl(Document doc) {
        // 策略1：优先从当前活跃的slide中获取图片（slick-active slick-current）
        Elements currentSlideImages = doc.select("div.slick-track div.slick-slide.slick-current div.index_imgBox__PdX6V img");
        if (!currentSlideImages.isEmpty()) {
            for (Element img : currentSlideImages) {
                String src = img.attr("src");
                if (StrUtil.isNotEmpty(src) && isValidImageUrl(src)) {
                    return src;
                }
            }
        }

        // 策略2：从所有活跃的slide中获取图片（slick-active）
        Elements activeSlideImages = doc.select("div.slick-track div.slick-slide.slick-active div.index_imgBox__PdX6V img");
        if (!activeSlideImages.isEmpty()) {
            for (Element img : activeSlideImages) {
                String src = img.attr("src");
                if (StrUtil.isNotEmpty(src) && isValidImageUrl(src)) {
                    return src;
                }
            }
        }

        // 策略3：从非克隆的slide中获取图片（排除slick-cloned）
        Elements nonClonedSlideImages = doc.select("div.slick-track div.slick-slide:not(.slick-cloned) div.index_imgBox__PdX6V img");
        if (!nonClonedSlideImages.isEmpty()) {
            for (Element img : nonClonedSlideImages) {
                String src = img.attr("src");
                if (StrUtil.isNotEmpty(src) && isValidImageUrl(src)) {
                    return src;
                }
            }
        }

        // 策略4：从轮播容器中的第一个图片容器获取（无论是否克隆）
        Elements slickTrackImages = doc.select("div.slick-track div.index_imgBox__PdX6V img");
        if (!slickTrackImages.isEmpty()) {
            for (Element img : slickTrackImages) {
                String src = img.attr("src");
                if (StrUtil.isNotEmpty(src) && isValidImageUrl(src)) {
                    return src;
                }
            }
        }

        // 策略5：直接通过图片容器class获取
        Elements imgBoxImages = doc.select("div.index_imgBox__PdX6V img");
        if (!imgBoxImages.isEmpty()) {
            for (Element img : imgBoxImages) {
                String src = img.attr("src");
                if (StrUtil.isNotEmpty(src) && isValidImageUrl(src)) {
                    return src;
                }
            }
        }

        // 策略6：查找轮播区域内的所有图片元素并遍历
        Elements slickTrackContainer = doc.select("div.slick-track");
        if (!slickTrackContainer.isEmpty()) {
            for (Element container : slickTrackContainer) {
                // 遍历轮播容器内的所有img标签
                Elements allImages = container.select("img");
                for (Element img : allImages) {
                    String src = img.attr("src");
                    if (StrUtil.isNotEmpty(src) && isValidImageUrl(src)) {
                        return src;
                    }
                }
            }
        }

        // 策略7：直接搜索澎湃新闻域名的图片
        Elements thePaperImages = doc.select("img[src*='imgpai.thepaper.cn']");
        if (!thePaperImages.isEmpty()) {
            for (Element img : thePaperImages) {
                String src = img.attr("src");
                if (StrUtil.isNotEmpty(src) && isValidImageUrl(src)) {
                    return src;
                }
            }
        }

        // 策略8：搜索包含thepaper.cn的图片
        Elements allThePaperImages = doc.select("img[src*='thepaper.cn']");
        if (!allThePaperImages.isEmpty()) {
            for (Element img : allThePaperImages) {
                String src = img.attr("src");
                if (StrUtil.isNotEmpty(src) && isValidImageUrl(src)) {
                    return src;
                }
            }
        }

        // 策略9：从正文内容区域获取图片
        Elements contentImages = doc.select("div.index_cententWrap__Jv8jK img");
        if (!contentImages.isEmpty()) {
            for (Element img : contentImages) {
                String src = img.attr("src");
                if (StrUtil.isNotEmpty(src) && isValidImageUrl(src)) {
                    return src;
                }
            }
        }

        // 策略10：最后备用策略 - 获取页面中任何有效的图片
        Elements allImages = doc.select("img");
        if (!allImages.isEmpty()) {
            for (Element img : allImages) {
                String src = img.attr("src");
                if (StrUtil.isNotEmpty(src) && isValidImageUrl(src)) {
                    return src;
                }
            }
        }

        return "无图片";
    }

    /**
     * 验证图片URL是否有效
     */
    private boolean isValidImageUrl(String url) {
        if (StrUtil.isEmpty(url)) {
            return false;
        }

        // 确保是完整的HTTP URL
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }

        // 排除明显的占位符和无效链接
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("placeholder") ||
                lowerUrl.contains("defhead") ||
                lowerUrl.contains("default") ||
                lowerUrl.contains("avatar") ||
                lowerUrl.contains("logo") ||
                url.length() < 20) { // 最小长度要求
            return false;
        }

        // 优先接受澎湃新闻域名的图片
        if (url.contains("imgpai.thepaper.cn") || url.contains("thepaper.cn")) {
            return true;
        }

        // 检查是否是常见的图片格式
        String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"};
        for (String ext : imageExtensions) {
            if (lowerUrl.contains(ext)) {
                return true;
            }
        }

        // 如果没有明显的图片扩展名，但URL看起来像图片服务，也接受
        if (lowerUrl.contains("image") || lowerUrl.contains("img") || lowerUrl.contains("pic")) {
            return true;
        }

        return false;
    }

    /**
     * 新闻链接实体类（包含URL和预览图片）
     */
    private static class NewsLink {
        String url = "";
        String previewImageUrl = "";
    }

    /**
     * 新闻内容实体类
     */
    private static class NewsContent {
        String title = "";
        String content = "";
        String imageUrl = "";
    }
}
