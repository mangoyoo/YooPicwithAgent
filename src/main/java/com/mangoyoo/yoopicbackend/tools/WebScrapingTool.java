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
import java.util.ArrayList;
import java.util.List;

import java.net.URL;
import java.net.MalformedURLException;
@Component
public class WebScrapingTool {

    @Tool(description = "search images by keyword from Internet")
    public String scrapeImagesByKeyword(@ToolParam(description = "Keyword to search for images") String keyword,
                                        @ToolParam(description = "Number of images to extract") Integer count) {
        try {
            // 限制最大数量
            if (count > 8) {
                return "Error: Maximum 30 images allowed";
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

            // 存储图片URL
            List<String> imageUrls = new ArrayList<>();

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

                    imageUrls.add(fileUrl);

                    // 达到指定数量就停止
                    if (imageUrls.size() >= count) {
                        break;
                    }
                } catch (Exception e) {
                    // 跳过处理失败的图片
                    continue;
                }
            }

            // 如果没有找到图片
            if (imageUrls.isEmpty()) {
                return "Error: No images found for keyword: " + keyword;
            }

            // 用逗号分隔拼接URL并返回
            return String.join(",", imageUrls);

        } catch (IOException e) {
            return "Error scraping images: " + e.getMessage();
        } catch (Exception e) {
            return "Error processing request: " + e.getMessage();
        }
    }


}

