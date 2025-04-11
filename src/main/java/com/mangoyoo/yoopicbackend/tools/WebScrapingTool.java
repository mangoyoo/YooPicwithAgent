package com.mangoyoo.yoopicbackend.tools;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

@Component
public class WebScrapingTool {

    @Tool(description = "search images by keyword from Internet")
    public String scrapeImagesByKeyword(@ToolParam(description = "Keyword to search for images") String keyword,
                                        @ToolParam(description = "Number of images to extract (default 1)") Integer count, @ToolParam(description = "A summary of completed steps and explanation of the next steps in Chinese") String summary) {
        try {
            // 限制最大数量
            if (count > 8) {
                return "Error: Maximum 8 images allowed";
            }

            // 构建Bing图片搜索URL
            String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", keyword);

            // 获取页面内容
            Document document = Jsoup.connect(fetchUrl).get();

            // 找到包含图片的容器
            Element div = document.getElementsByClass("dgControl").first();
            if (div == null) {
                return "Error: Unable to find image container";
            }

            // 获取所有图片链接元素
            Elements linkElements = div.select("a.iusc");

            // 存储所有可能的图片URL
            List<String> allImageUrls = new ArrayList<>();

            // 遍历链接元素提取图片URL
            for (Element linkElement : linkElements) {
                try {
                    // 获取m属性中的JSON数据
                    String mAttr = linkElement.attr("m");
                    if (mAttr == null || mAttr.trim().isEmpty()) {
                        continue;
                    }

                    // 解析JSON获取原始图片URL
                    JSONObject jsonObject = JSONUtil.parseObj(mAttr);
                    String fileUrl = jsonObject.getStr("murl");

                    if (fileUrl == null || fileUrl.trim().isEmpty()) {
                        continue;
                    }

                    // 处理图片URL，去除查询参数
                    int questionMarkIndex = fileUrl.indexOf("?");
                    if (questionMarkIndex > -1) {
                        fileUrl = fileUrl.substring(0, questionMarkIndex);
                    }

                    allImageUrls.add(fileUrl);
                } catch (Exception e) {
                    // 跳过处理失败的图片
                    continue;
                }
            }

            // 如果没有找到图片
            if (allImageUrls.isEmpty()) {
                return "Error: No images found for keyword: " + keyword;
            }

            // 随机打乱URL列表
            Collections.shuffle(allImageUrls);

            // 存储验证通过的图片URL
            List<String> validImageUrls = new ArrayList<>();

            // 验证每个URL是否可以下载
            for (String imageUrl : allImageUrls) {
                if (validImageUrls.size() >= count) {
                    break;
                }

                if (isUrlDownloadable(imageUrl)) {
                    validImageUrls.add(imageUrl);
                }
            }

            // 如果没有找到可下载的图片
            if (validImageUrls.isEmpty()) {
                return "Error: No downloadable images found for keyword: " + keyword;
            }

            // 用逗号分隔拼接URL并返回
            return String.join(",", validImageUrls);

        } catch (IOException e) {
            return "Error scraping images: " + e.getMessage();
        } catch (Exception e) {
            return "Error processing request: " + e.getMessage();
        }
    }

    /**
     * 检查URL是否可以成功下载
     */
    private boolean isUrlDownloadable(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 设置请求方法为HEAD，只获取响应头
            connection.setRequestMethod("HEAD");

            // 设置超时时间
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // 设置User-Agent避免被反爬虫
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            // 获取响应码
            int responseCode = connection.getResponseCode();

            // 检查响应码是否表示成功
            boolean isSuccess = responseCode >= 200 && responseCode < 300;

            // 检查Content-Type是否为图片
            String contentType = connection.getContentType();
            boolean isImage = contentType != null && contentType.startsWith("image/");

            connection.disconnect();

            return isSuccess && isImage;

        } catch (Exception e) {
            return false;
        }
    }
}
