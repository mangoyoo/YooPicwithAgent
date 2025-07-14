package com.mangoyoo.yoopicbackend.app;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.MessageFormat;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.mangoyoo.yoopicbackend.advisor.MyLoggerAdvisor;
import com.mangoyoo.yoopicbackend.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Component
@Slf4j
public class DefaultExpert {
    private ToolCallback[] allTools;
    private final ChatClient chatClient;
    private List<ToolCallback> totalTools;
    private ToolCallbackProvider toolCallbackProvider;
    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";

    private static final String DEFAULT_MULTIMODEL = "qwen-vl-plus";

    public DefaultExpert(ChatModel dashscopeChatModel, ToolCallback[] allTools, ToolCallbackProvider toolCallbackProvider) {
        this.allTools=allTools;
        this.toolCallbackProvider=toolCallbackProvider;
        //  先获取 ToolCallbackProvider 中的工具，然后合并
        totalTools= new ArrayList<>();
// 添加 availableTools（假设它是 ToolCallback[] 类型）
        totalTools.addAll(Arrays.asList(this.allTools));
// 添加 toolCallbackProvider 提供的工具
        totalTools.addAll(Arrays.asList(toolCallbackProvider.getToolCallbacks()));
        // 创建聊天内存
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20) // 设置最大消息数
                .build();

        // 修改：使用builder模式创建MessageChatMemoryAdvisor
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志 Advisor，可按需开启
                        new MyLoggerAdvisor()
                )
                .build();
    }

    public String doChat(String message, String chatId) {
        // 对于纯文本对话，使用文本模型而不是VL模型
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .options(DashScopeChatOptions.builder()
                        .withModel("qwen-turbo")  // 指定使用文本模型
                        .withTemperature(0.8)
                        .build())
                .advisors(spec -> spec.param(CONVERSATION_ID, chatId)
                        .param("topK", 20))
                .toolCallbacks(totalTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        return content;
    }


    public String doChatWithTools(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CONVERSATION_ID, chatId)
                        .param("topK", 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        return content;
    }

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CONVERSATION_ID, chatId)
                        .param("topK", 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        return content;
    }

    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CONVERSATION_ID, chatId)
                        .param("topK", 10))
                .stream()
                .content();
    }

    /**
     * 带图片的对话 - 通过URL方式
     * @param message 文本消息
     * @param imageUrl 图片URL
     * @param chatId 会话ID
     * @return 响应内容
     */
    public String doChatWithImage(String message, String imageUrl, String chatId) {
        try {
            List<Media> mediaList = List.of(new Media(MimeTypeUtils.IMAGE_JPEG,
                    new URI(imageUrl).toURL().toURI()));

            UserMessage userMessage = UserMessage.builder()
                    .text(message)
                    .media(mediaList)
                    .metadata(new HashMap<>())
                    .build();
            userMessage.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT, MessageFormat.IMAGE);

            ChatResponse response = chatClient
                    .prompt(new Prompt(userMessage,
                            DashScopeChatOptions.builder()
                                    .withModel("qvq-max-latest")
                                    .withMultiModel(true)
                                    .build()))
                    .advisors(spec -> spec.param(CONVERSATION_ID, chatId)
                            .param("topK", 10))
                    .call()
                    .chatResponse();

            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("处理图片对话时发生错误", e);
            return "抱歉，处理图片时发生错误，请稍后重试。";
        }
    }

    /**
     * 带图片的对话 - 通过Resource方式（本地文件）
     * @param message 文本消息
     * @param imageResource 图片资源
     * @param chatId 会话ID
     * @return 响应内容
     */
    public String doChatWithImageResource(String message, Resource imageResource, String chatId) {
        try {
            List<Media> mediaList = List.of(new Media(MimeTypeUtils.IMAGE_JPEG, (org.springframework.core.io.Resource) imageResource));

            UserMessage userMessage = UserMessage.builder()
                    .text(message)
                    .media(mediaList)
                    .metadata(new HashMap<>())
                    .build();
            userMessage.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT, MessageFormat.IMAGE);

            ChatResponse response = chatClient
                    .prompt(new Prompt(userMessage,
                            DashScopeChatOptions.builder()
                                    .withModel(DEFAULT_MULTIMODEL)
                                    .withMultiModel(true)
                                    .build()))
                    .advisors(spec -> spec.param(CONVERSATION_ID, chatId)
                            .param("topK", 10))
                    .call()
                    .chatResponse();

            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("处理图片对话时发生错误", e);
            return "抱歉，处理图片时发生错误，请稍后重试。";
        }
    }

    /**
     * 带图片的对话 - 流式响应
     * @param message 文本消息
     * @param imageUrl 图片URL
     * @param chatId 会话ID
     * @return 流式响应
     */
    /**
     * 带图片的对话 - 流式响应
     * @param message 文本消息
     * @param imageUrl 图片URL
     * @param chatId 会话ID
     * @return 流式响应
     */
    public Flux<String> doChatWithImageByStream(String message, String imageUrl, String chatId) {
        try {
            log.info("=== MyApp图片流式对话开始 ===");
            log.info("message: {}", message);
            log.info("imageUrl: {}", imageUrl);
            log.info("chatId: {}", chatId);

            List<Media> mediaList = List.of(new Media(MimeTypeUtils.IMAGE_JPEG,
                    new URI(imageUrl).toURL().toURI()));
            log.info("媒体列表创建成功，数量: {}", mediaList.size());

            UserMessage userMessage = UserMessage.builder()
                    .text(message)
                    .media(mediaList)
                    .metadata(new HashMap<>())
                    .build();
            userMessage.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT, MessageFormat.IMAGE);
            log.info("用户消息构建完成");

            DashScopeChatOptions options = DashScopeChatOptions.builder()
                    .withModel(DEFAULT_MULTIMODEL)
                    .withMultiModel(true)
                    .build();
            log.info("DashScope选项配置: model={}, multiModel={}", DEFAULT_MULTIMODEL, true);

            Prompt prompt = new Prompt(userMessage, options);
            log.info("Prompt创建完成");

            return chatClient
                    .prompt(prompt)
                    .advisors(spec -> spec.param(CONVERSATION_ID, chatId)
                            .param("topK", 10))
                    .stream()
                    .content()
                    .doOnSubscribe(subscription -> {
                        log.info("ChatClient流订阅开始");
                    })
                    .doOnNext(content -> {
                        log.debug("ChatClient返回内容: {}", content);
                    })
                    .doOnComplete(() -> {
                        log.info("ChatClient流完成");
                    })
                    .doOnError(error -> {
                        log.error("ChatClient流发生错误", error);
                    });

        } catch (Exception e) {
            log.error("=== MyApp图片流式对话异常 ===", e);
            return Flux.just("抱歉，处理图片时发生错误：" + e.getMessage());
        }
    }

    // DefaultExpert.java - 新增同步方法
    public String doChatWithImageSync(String message, String imageUrl, String chatId) {
        try {
            log.info("=== MyApp图片同步对话开始 ===");

            List<Media> mediaList = List.of(new Media(MimeTypeUtils.IMAGE_JPEG,
                    new URI(imageUrl).toURL().toURI()));
            UserMessage userMessage = UserMessage.builder()
                    .text(message)
                    .media(mediaList)
                    .metadata(new HashMap<>())
                    .build();
            userMessage.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT, MessageFormat.IMAGE);

            DashScopeChatOptions options = DashScopeChatOptions.builder()
                    .withModel(DEFAULT_MULTIMODEL)
                    .withMultiModel(true)
                    .build();

            Prompt prompt = new Prompt(userMessage, options);

            // 使用call()而不是stream()进行同步调用
            ChatResponse response = chatClient
                    .prompt(prompt)
                    .advisors(spec -> spec.param(CONVERSATION_ID, chatId)
                            .param("topK", 20))
                    .toolCallbacks(totalTools)
                    .call()
                    .chatResponse();

            // 从响应中提取完整文本
            return response.getResults().get(0).getOutput().getText();
        } catch (Exception e) {
            log.error("MyApp图片同步对话异常", e);
            return "抱歉，处理图片时发生错误：" + e.getMessage();
        }
    }
    /**
     * 带图片和工具的对话
     * @param message 文本消息
     * @param imageUrl 图片URL
     * @param chatId 会话ID
     * @return 响应内容
     */
    public String doChatWithImageAndTools(String message, String imageUrl, String chatId) {
        try {
            List<Media> mediaList = List.of(new Media(MimeTypeUtils.IMAGE_JPEG,
                    new URI(imageUrl).toURL().toURI()));

            UserMessage userMessage = UserMessage.builder()
                    .text(message)
                    .media(mediaList)
                    .metadata(new HashMap<>())
                    .build();
            userMessage.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT, MessageFormat.IMAGE);

            ChatResponse response = chatClient
                    .prompt(new Prompt(userMessage,
                            DashScopeChatOptions.builder()
                                    .withModel(DEFAULT_MULTIMODEL)
                                    .withMultiModel(true)
                                    .build()))
                    .advisors(spec -> spec.param(CONVERSATION_ID, chatId)
                            .param("topK", 10))
                    .advisors(new MyLoggerAdvisor())
                    .tools(allTools)
                    .call()
                    .chatResponse();

            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("处理图片和工具对话时发生错误", e);
            return "抱歉，处理图片时发生错误，请稍后重试。";
        }
    }
}
