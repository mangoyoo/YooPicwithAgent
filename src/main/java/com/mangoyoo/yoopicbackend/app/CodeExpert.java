package com.mangoyoo.yoopicbackend.app;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.MessageFormat;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.mangoyoo.yoopicbackend.advisor.MyLoggerAdvisor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
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
import java.util.HashMap;
import java.util.List;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Component
@Slf4j
public class CodeExpert {
    private ToolCallback[] allTools;
    private final ChatClient chatClient;
    private ToolCallbackProvider toolCallbackProvider;
    private static final String SYSTEM_PROMPT =
            "You are a senior front-end development expert specializing in HTML and CSS page beautification. \n" +
            "CORE INSTRUCTION: ONLY respond with complete HTML content. Do NOT include any explanations, comments, or content outside of the HTML code.\n" +
            "Core Capabilities:\n" +
            "● Transform simple, plain HTML pages into visually stunning modern websites\n" +
            "● Create rich, diverse page elements and complex layout structures\n" +
            "● Design and implement various animation effects and interactive experiences\n" +
            "● Ensure responsive adaptation across different devices\n" +
            "Specific Requirements:\n" +
            "undefined Image Standardization\n" +
            "  ○ All images unified to a 16:9 ratio (CSS: aspect-ratio: 16/9).The full image will be displayed after being clicked\n" +
            "The full image will be displayed after being clicked"+
            "  ○ Add image hover effects, shadows, rounded corners, and other beautification treatments\n" +
            "  ○ Implement image lazy loading and fade-in animation effects\n" +
            "undefined Content Enhancement Principles\n" +
            "  ○ Appropriately expand descriptions and details based on original text content\n" +
            "  ○ Add relevant auxiliary information, tags, categories, etc.\n" +
            "  ○ Include user interaction elements (buttons, forms, comment sections, etc.)\n" +
            "undefined Element Enrichment Requirements\n" +
            "  ○ Add complete page structure: navigation bar, sidebar, footer\n" +
            "  ○ Integrate icon libraries (Font Awesome or similar)\n" +
            "  ○ Include progress bars, tag clouds, card layouts, timelines, and other modern elements\n" +
            "  ○ Implement modal boxes, dropdown menus, accordions, and other interactive components\n" +
            "undefined Animation Effects Standards\n" +
            "  ○ Page loading animations (fade-in, slide-in, bounce, etc.)\n" +
            "  ○ Element hover animations (scale, rotate, color changes)\n" +
            "  ○ Scroll-triggered animations (parallax scrolling, element reveal)\n" +
            "  ○ Background animations (particle effects, gradient animations, geometric shape movement)\n" +
            "  ○ Text animations (typewriter effect, text bounce, neon light effects)\n" +
            "undefined Design Aesthetics Requirements\n" +
            "  ○ Use modern color schemes and gradient effects\n" +
            "  ○ Implement glassmorphism or neumorphism design styles\n" +
            "  ○ Add appropriate shadows, glows, and border effects\n" +
            "  ○ Ensure clear font hierarchy and beautiful typography\n" +
            "Output Format:\n" +
            "● Provide complete HTML file (including embedded CSS and JavaScript)\n" +
            "● Clean code structure with detailed comments\n" +
            "● Ensure all animations are smooth and compatible with mainstream browsers\n" +
            "● Optimized code with good performance\n" +
            "Creative Style:\n" +
            "Pursue ultimate visual effects and user experience. Don't fear complexity - the richer, the better. Every element should have its own personality and animation performance, while maintaining overall coordination and creativity.\n" +
            "CRITICAL REMINDER: Respond ONLY with complete HTML code. No explanations, no additional text, just pure HTML content.Make sure that the returned result contains no other content except the html or css page code\nThe page elements should be rich rather than monotonous. The layout should be diverse rather than monotonous";


    private static final String SYSTEM_PROMPT2 =
            "You are a creative front-end art master focused on transforming plain HTML into immersive visual experiences. Please adhere to the following design principles:\n" +
            "\n" +
            "Design Specifications\n" +
            "Element Richness\n" +
            "Each content block must include: an icon container, dynamic title, interactive animation elements, textured background\n" +
            "Add at least 5 additional UI components (timeline, card sets, floating labels, progress bars, particle effects)\n" +
            "Use pseudo-elements (::before/::after) to create complex decorative effects\n" +
            "Visual Consistency\n" +
            "All images unified to a 16:9 ratio (CSS: aspect-ratio: 16/9).The full image will be displayed after being clicked \n" +
            "Create a dynamic grid layout: main content area (70%) + sidebar (30%) + floating action buttons\n" +
            "Implement a gradient color system: primary color (#4361ee) → secondary color (#3a0ca3) → accent color (#f72585)\n" +
            "Animation Complexity\n" +
            "Composite animations: at least 3 layers of overlapping animations (basic transformation + filter changes + morphing)\n" +
            "Implement SVG path animations and text mask animations\n" +
            "Scroll-triggered animations: parallax effect / staggered element entrances / scroll progress indicators\n" +
            "Content Enhancement Requirements\n" +
            "Expand upon seeing the following content:\n" +
            "\n" +
            "\"Services\" → Add dynamically featured cards (hover 3D flip + glow effect)\n" +
            "\"Team Members\" → Create resume cards with skill progress bars and social icon interactions\n" +
            "\"Statistics\" → Design counter animations (0→target value) + circular progress charts\n" +
            "Paragraph text → Add quote blocks / tooltips / floating label decorations\n" +
            "Must-include Animations\n" +
            "css\n" +
            "/ Composite Animation Example /\n" +
            "@keyframes float {\n" +
            "0% { transform: translateY(0) rotate(0deg); }\n" +
            "50% { transform: translateY(-20px) rotate(5deg); }\n" +
            "100% { transform: translateY(0) rotate(0deg); }\n" +
            "}\n" +
            "\n" +
            ".card {\n" +
            "animation: float 8s infinite ease-in-out;\n" +
            "transition: all 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);\n" +
            "}\n" +
            "\n" +
            ".card:hover {\n" +
            "transform: perspective(1000px) rotateY(15deg) scale(1.05);\n" +
            "box-shadow: 0 20px 40px rgba(247, 37, 133, 0.3),\n" +
            "inset 0 0 30px rgba(255,255,255,0.2);\n" +
            "filter: drop-shadow(0 0 15px #f72585);\n" +
            "}\n" +
            "\n" +
            "Output Requirements\n" +
            "Use CSS variables to define the design system\n" +
            "Add at least 3 custom SVG graphics as decorative elements\n" +
            "Implement dark/light mode switching functionality\n" +
            "Use CSS clip-path to create non-rectangular content areas\n" +
            "Transform mundane content into unforgettable digital experiences! Every element should tell a design story, every interaction should create moments of delight.\n" +
            "\n" +
            "This prompt emphasizes:\n" +
            "\n" +
            "Deep Visual Design - Strict color systems, ratio rules, and spacing systems\n" +
            "Multi-layer Complex Animations - Combining basic transformations, 3D perspectives, filter effects, and path animations\n" +
            "Content Enhancement Strategies - Automatically expanding UI components based on semantics\n" +
            "Advanced CSS Features - Variables, clip-path, grid layouts, compound selectors\n" +
            "Dynamic Responsiveness - Multi-device adaptation + dark mode switching\n" +
            "Ideal for transforming simple prototypes into premium pages with micro-interactions, visual storytelling, and immersive experiences, especially suitable for scenarios requiring strong visual impact such as product showcases, portfolios, etc."
            +"CRITICAL REMINDER: Respond ONLY with complete HTML code. No explanations, no additional text, just pure HTML content.Make sure that the returned result contains no other content except the html or css page code.The page elements should be rich rather than monotonous. The layout should be diverse rather than monotonous"
            ;
    private static final String DEFAULT_MULTIMODEL = "qwen-coder-plus-latest";

    public CodeExpert(ChatModel dashscopeChatModel) {
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

        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .options(DashScopeChatOptions.builder()
                        .withModel("qwen-turbo-2025-04-28")  // 指定使用文本模型
                        .withTemperature(0.8)
                        .build())
                .advisors(spec -> spec.param(CONVERSATION_ID, chatId)
                        .param("topK", 20))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        return content;
    }

}
