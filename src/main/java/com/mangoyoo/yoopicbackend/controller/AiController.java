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

import java.util.concurrent.CompletableFuture;

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

//    @GetMapping("/manus/chat/see")
//    public SseEmitter doChatWithManus(String message) {
//        log.info("开始agent对话");
//        MyManus myManus = new MyManus(allTools, dashscopeChatModel);
////        return myManus.runStream(message);
//    }
@GetMapping("/manus/chat/see")
public SseEmitter doChatWithManus(String message) {
    log.info("开始agent对话，测试文件下载功能");

    SseEmitter emitter = new SseEmitter(60000L); // 60秒超时

    // 使用线程池异步处理，模拟流式响应
    CompletableFuture.runAsync(() -> {
        try {
            // 模拟思考时间
            Thread.sleep(500);

            // 发送开始回复的消息
            emitter.send("正在为您");
            Thread.sleep(300);

            emitter.send("生成文件");
            Thread.sleep(300);

            emitter.send("，请稍等...");
            Thread.sleep(800);

            // 发送文件下载链接
            emitter.send("文件下载链接是：http://www.yoodns.yoopic.space/public/1920742554046758914/2025-05-09_Kpxongvbu39zgXxV.webp");
            Thread.sleep(500);

            // 发送结束标志
            emitter.send("[DONE]");
            emitter.complete();

        } catch (Exception e) {
            log.error("SSE发送失败", e);
            emitter.completeWithError(e);
        }
    });

    // 设置异常处理
    emitter.onError(throwable -> {
        log.error("SSE连接错误", throwable);
        emitter.complete();
    });

    // 设置完成处理
    emitter.onCompletion(() -> {
        log.info("SSE连接完成");
    });

    return emitter;
}

}

