package com.mangoyoo.yoopicbackend.Manus;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

@Component
public class ManusFactory {

    @Resource
    private ToolCallbackProvider toolCallbackProvider; // MCP工具会自动注入到这里

    private final ToolCallback[] allTools;
    private final ChatModel dashscopeChatModel;

    public ManusFactory(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        this.allTools = allTools;
        this.dashscopeChatModel = dashscopeChatModel;
    }

    public MyManus createManus() {
        // 将自动注入的MCP工具传递给构造函数
        return new MyManus(dashscopeChatModel, allTools, toolCallbackProvider);
    }
}
