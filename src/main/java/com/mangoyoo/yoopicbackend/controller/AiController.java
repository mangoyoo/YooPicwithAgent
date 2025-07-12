package com.mangoyoo.yoopicbackend.controller;


import com.mangoyoo.yoopicbackend.Manus.MyManus;
import com.mangoyoo.yoopicbackend.app.MyApp;
import com.mangoyoo.yoopicbackend.manager.upload.ChatFileUpload;
import com.mangoyoo.yoopicbackend.manager.upload.DefaultChatFileUpload;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {
    @Resource
    DefaultChatFileUpload chatMultipartFileUpload;
    @Resource
    private MyApp myApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @GetMapping("/my_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return myApp.doChat(message, chatId);
    }

    @PostMapping(value = "/my_app/chat/sse",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String doChatWithLoveAppSSE(
            @RequestPart("message") String message,
            @RequestPart("chatId") String chatId,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {

        try {
            log.info("=== 聊天请求开始 - chatId: {} ===", chatId);

            // 参数验证
            if (message == null || message.trim().isEmpty()) {
                return "消息内容不能为空";
            }

            if (chatId == null || chatId.trim().isEmpty()) {
                return "会话ID不能为空";
            }

            // 有文件的情况
            if (file != null && !file.isEmpty()) {
                log.info("处理图片文件: {}", file.getOriginalFilename());

                // 文件大小检查
                if (file.getSize() > 10 * 1024 * 1024) { // 10MB限制
                    return "文件大小不能超过10MB";
                }

                // 文件类型检查
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return "只支持图片文件";
                }

                String imageUrl = null;
                try {
                    // 上传文件
                    imageUrl = chatMultipartFileUpload.uploadFile(file, "chat");
                    log.info("文件上传完成 - imageUrl: {}", imageUrl);

                    // URL格式验证
                    if (imageUrl == null || imageUrl.trim().isEmpty()) {
                        return "文件上传失败，URL为空";
                    }

                    if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                        return "图片URL格式错误";
                    }

                } catch (Exception uploadEx) {
                    log.error("文件上传失败", uploadEx);
                    return "文件上传失败：" + uploadEx.getMessage();
                }

                // 调用myApp的图片对话方法
                log.info("开始AI图片对话");
                return myApp.doChatWithImage(message, imageUrl, chatId);

            } else {
                // 无图片的普通对话
                log.info("开始普通文本对话");
                return myApp.doChat(message, chatId);
            }

        } catch (Exception e) {
            log.error("聊天请求处理失败 - chatId: {}", chatId, e);
            return "服务器内部错误：" + e.getMessage();
        }
    }





    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        MyManus myManus = new MyManus(allTools, dashscopeChatModel);
        return myManus.runStream(message);
    }

}

