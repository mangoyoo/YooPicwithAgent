package com.mangoyoo.yoopicbackend.Manus;
import com.mangoyoo.yoopicbackend.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class MyManus extends ToolCallAgent {
    public MyManus(ChatModel dashscopeChatModel ,ToolCallback[] allTools,ToolCallbackProvider toolCallbackProvider) {
        super(allTools,toolCallbackProvider);
        this.setToolCallbackProvider(toolCallbackProvider);
        this.setName("YooManus");
        String SYSTEM_PROMPT = """           
                You are YooPic-Agent, an all-capable AI Agent, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """      
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                If it is a regular conversation (such as  "hello"),you need to call the `doterminate` tool now.
                Before using each tool (excluding the doterminate tool), you should clearly explain the execution results and describe your next action. 
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                In your thinking at each step, you can refer to this example for your response structure (it does not involve specific information, only the structure):  'The previous step successfully collected hot news information. Next, I will search for the weather in Guangzhou. 
                If you want to stop the interaction at any point, use the `doterminate` tool/function call.
                Before using each tool, clearly explain the execution results and suggest the next steps.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(10);
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }

}
