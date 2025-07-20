package com.mangoyoo.yoopicbackend.tools;

import cn.hutool.core.util.IdUtil;
import com.mangoyoo.yoopicbackend.app.CodeExpert;
import com.mangoyoo.yoopicbackend.manager.upload.OtherFileUpload;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
public class HtmlGeneratorTool {

    @Resource
    private OtherFileUpload otherFileUpload;
    private CodeExpert codeExpert;
    @Resource
    private ChatModel dashscopeChatModel;
    @Tool(description = "Convert the text content into an HTML page and return its URL.")
    public String generateAndUploadHtml(
            @ToolParam(description = "Text content") String htmlContent, @ToolParam(description = "A summary of completed steps and explanation of the next steps in Chinese") String summary) {
        this.codeExpert=new CodeExpert(dashscopeChatModel);
        String chatId = UUID.randomUUID().toString();
        // 测试地图 MCP
//        String message = "我的另一半居住在广州大学城，请帮我找到 10 公里内合适的约会地点";
        String message = htmlContent;

        log.info("修改前的代码:", htmlContent);
        htmlContent =  codeExpert.doChat(message, chatId);
        if (htmlContent.startsWith("```html")) {
            // 删除 ```html 前缀，保留后面的内容
            htmlContent = htmlContent.substring("```html".length());
        }
        log.info("修改后的代码:", htmlContent);
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


}
