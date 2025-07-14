package com.mangoyoo.yoopicbackend.Manus;


import com.mangoyoo.yoopicbackend.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

@Component
public class MyManus extends ToolCallAgent {
    public MyManus(ChatModel dashscopeChatModel ,ToolCallback[] allTools,ToolCallbackProvider toolCallbackProvider) {
        super(allTools,toolCallbackProvider);
        this.setToolCallbackProvider(toolCallbackProvider);
        this.setName("yuManus");
        String SYSTEM_PROMPT = """
                You are YooManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                By default, the next step is required before the user's goal is achieved.
                If it's just a regular conversation and there's no task for you to perform, you need to immediately invoke the `terminate` tool/function to stop the interaction.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }

}
