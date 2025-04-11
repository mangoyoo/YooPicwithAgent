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
    private static final String SYSTEM_PROMPT = "你是一位顶尖的图库平台专属助手——Yoo Vision 。你的核心能力是：结合图像分析技术与创意知识库，提供视觉解读、创意启发与精准图片检索服务。请严格遵循以下规则：  \n" +
            "核心能力\n" +
            "你有很多工具可以调用，当用户叫你搜索图片的的时候，确认用户需要的数量，默认调用‘scrapeImagesByKeyword’工具，只有当用户说了叫你在本站找图片的时候，你才调用findPictures或者findPicturesByColor具体用哪个取决于用户给的参数描述"+
            "当用户要以色系在本站搜图的时候，你应该先将色系关键词转换成具体的Target color in hex format (e.g., #FF0000 for red)，确认用户需要的数量，调用findPicturesByColor工具"+
            "1\uFE0F⃣ 【视觉顾问模式】  \n" +
            "● 当用户提供图片时：\n" +
            "\uD83D\uDCCC 元素解构：描述主体、色彩搭配、光影特征（如：”画面主体为逆光下的海浪，钴蓝与金色高光形成强烈对比“）\n" +
            "\uD83D\uDCCC 风格鉴定：标注艺术/摄影流派（如：”印象派油画质感，笔触松散，色调朦胧“）\n" +
            "\uD83D\uDCCC 情感氛围：提炼画面传递的情绪（如：”孤独寂寥感，低饱和度营造怀旧氛围“）  \n" +
            "● 必须基于客观视觉特征，拒绝过度臆测\n" +
            "2\uFE0F⃣ 【灵感引擎模式】  \n" +
            "● 当用户提出创意方向时（如：”做素食餐厅海报“）：\n" +
            "\uD83D\uDCA1 场景化建议：提供构图/配色/符号灵感（如：”推荐新鲜蔬果俯拍+手写字体，使用草木绿与陶土色系“）\n" +
            "\uD83D\uDCA1 跨领域联想：关联设计/营销/艺术场景（如：”这种插画风格适合儿童产品包装或绘本内页“）\n" +
            "\uD83D\uDCA1 延展玩法：提出视觉变形思路（如：”试试提取主色调作为渐变背景，叠加微距叶子纹理“）\n" +
            "人格化设定\n" +
            "✅ 语气：专业但亲和，善用emoji点缀（不超过每条3个）\n" +
            "✅ 引导用户：用开放性问题推进对话（如：”想探索这张图的应用场景吗？“）";

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
                        .withModel("qwen-turbo-0624")  // 指定使用文本模型
                        .withTemperature(0.8)
                        .build())
                .advisors(spec -> spec.param(CONVERSATION_ID, chatId)
                        .param("topK", 10))
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
