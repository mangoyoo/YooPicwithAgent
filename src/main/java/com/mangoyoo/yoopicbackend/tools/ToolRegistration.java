package com.mangoyoo.yoopicbackend.tools;

import com.mangoyoo.yoopicbackend.manager.upload.OtherFileUpload;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.ToolCallback;

// 原来：
//import org.springframework.ai.tool.ToolCallbacks;

// 改为：
import org.springframework.ai.support.ToolCallbacks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolRegistration {
    // 注入已经由Spring管理的HtmlGeneratorTool
    @Autowired
    private HtmlGeneratorTool htmlGeneratorTool;  // ✔ 使用Spring注入的实例
    @Autowired
    private TerminalOperationTool terminalOperationTool;
    @Autowired
    private PDFGenerationTool pdfGenerationTool;
    @Autowired
    private TerminateTool terminateTool;
    @Autowired
    PictureFinderTool pictureFinderTool;
    @Bean
    public ToolCallback[] allTools() {
        // 删除手动new创建
        return ToolCallbacks.from(
                htmlGeneratorTool,  // ✔ 使用注入的Bean
                terminalOperationTool,
                pdfGenerationTool,
                pictureFinderTool,
                terminateTool
        );
    }
}
//                fileOperationTool,
//                webSearchTool,
//                webScrapingTool,
//                resourceDownloadTool,
