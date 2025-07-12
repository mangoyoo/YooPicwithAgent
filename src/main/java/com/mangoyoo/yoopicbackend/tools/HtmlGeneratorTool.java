package com.mangoyoo.yoopicbackend.tools;

import cn.hutool.core.util.IdUtil;
import com.mangoyoo.yoopicbackend.manager.upload.OtherFileUpload;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class HtmlGeneratorTool {

    @Resource
    private OtherFileUpload otherFileUpload;

    @Tool(description = "Convert the HTML code into the .html file and return its URL.")
    public String generateAndUploadHtml(
            @ToolParam(description = "HTML code to be written to file") String htmlContent) {

        try {
            // 1. 验证HTML内容
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                return "Error: HTML content cannot be empty";
            }

            // 2. 生成唯一的文件名
            String fileName = "generated_" + IdUtil.simpleUUID() + ".html";

            log.info("开始生成HTML文件: {}", fileName);

            // 3. 直接从HTML内容创建MultipartFile
            byte[] htmlBytes = htmlContent.getBytes(StandardCharsets.UTF_8);
            MultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    fileName,
                    "text/html",
                    htmlBytes
            );

            log.info("HTML文件创建成功，文件大小: {} bytes", htmlBytes.length);

            // 4. 上传到第三方存储
            log.info("开始上传HTML文件到云存储");
            String uploadUrl = otherFileUpload.uploadFile(multipartFile, "html");

            log.info("HTML文件上传成功，URL: {}", uploadUrl);

            return uploadUrl;

        } catch (Exception e) {
            log.error("生成或上传HTML文件失败", e);
            return "Error generating or uploading HTML file: " + e.getMessage();
        }
    }

    /**
     * 生成带自定义文件名的HTML文件并上传（内部方法）
     */
    public String generateAndUploadHtmlWithFilename(String htmlContent, String customFilename) {

        try {
            // 1. 验证HTML内容
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                return "Error: HTML content cannot be empty";
            }

            // 2. 验证自定义文件名
            if (customFilename == null || customFilename.trim().isEmpty()) {
                return "Error: Custom filename cannot be empty";
            }

            // 3. 使用自定义文件名
            String fileName = customFilename.trim() + ".html";

            log.info("开始生成HTML文件: {}", fileName);

            // 4. 直接从HTML内容创建MultipartFile
            byte[] htmlBytes = htmlContent.getBytes(StandardCharsets.UTF_8);
            MultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    fileName,
                    "text/html",
                    htmlBytes
            );

            log.info("HTML文件创建成功，文件大小: {} bytes", htmlBytes.length);

            // 5. 上传到第三方存储
            log.info("开始上传HTML文件到云存储");
            String uploadUrl = otherFileUpload.uploadFile(multipartFile, "html");

            log.info("HTML文件上传成功，URL: {}", uploadUrl);

            return uploadUrl;

        } catch (Exception e) {
            log.error("生成或上传HTML文件失败", e);
            return "Error generating or uploading HTML file: " + e.getMessage();
        }
    }

    /**
     * 验证HTML内容（内部方法）
     */
    public String validateHtmlContent(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return "Error: HTML content cannot be empty";
        }

        // 基本的HTML验证
        String lowerContent = htmlContent.toLowerCase();
        if (!lowerContent.contains("<html") && !lowerContent.contains("<!doctype")) {
            return "Warning: Content may not be valid HTML (missing <html> tag or DOCTYPE declaration)";
        }

        // 检查基本的HTML结构
        if (lowerContent.contains("<html") && !lowerContent.contains("</html>")) {
            return "Warning: HTML tag is not properly closed";
        }

        if (lowerContent.contains("<head") && !lowerContent.contains("</head>")) {
            return "Warning: HEAD tag is not properly closed";
        }

        if (lowerContent.contains("<body") && !lowerContent.contains("</body>")) {
            return "Warning: BODY tag is not properly closed";
        }

        return "HTML content validation passed";
    }

    /**
     * 生成完整HTML页面（内部方法）
     */
    public String generateCompleteHtmlPage(String title, String bodyContent, String cssStyles) {

        try {
            StringBuilder htmlBuilder = new StringBuilder();

            // 构建完整的HTML页面
            htmlBuilder.append("<!DOCTYPE html>\n");
            htmlBuilder.append("<html lang=\"zh-CN\">\n");
            htmlBuilder.append("<head>\n");
            htmlBuilder.append("    <meta charset=\"UTF-8\">\n");
            htmlBuilder.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            htmlBuilder.append("    <title>").append(title != null ? title : "Generated Page").append("</title>\n");

            // 添加CSS样式
            if (cssStyles != null && !cssStyles.trim().isEmpty()) {
                htmlBuilder.append("    <style>\n");
                htmlBuilder.append("        ").append(cssStyles).append("\n");
                htmlBuilder.append("    </style>\n");
            }

            htmlBuilder.append("</head>\n");
            htmlBuilder.append("<body>\n");
            htmlBuilder.append("    ").append(bodyContent != null ? bodyContent : "<h1>Welcome</h1>").append("\n");
            htmlBuilder.append("</body>\n");
            htmlBuilder.append("</html>");

            String htmlContent = htmlBuilder.toString();

            // 上传生成的HTML页面
            return generateAndUploadHtml(htmlContent);

        } catch (Exception e) {
            log.error("生成完整HTML页面失败", e);
            return "Error generating complete HTML page: " + e.getMessage();
        }
    }
}
