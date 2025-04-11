package com.mangoyoo.yoopicbackend.Manus;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest
class MyManusTest {

    @Autowired
    private MyManus myManus;

    @Test
    void run() {
        String userPrompt = """  
               帮我获取今天的top 3热点新闻，还有广州的天气，做成html文件返回给我   
                """;
        String answer = myManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
    @Test
    void runStream() {
        String userPrompt = """  
           帮我在本站找2张标签为表情包的图片，返回url给我   
            """;

        SseEmitter emitter = myManus.runStream(userPrompt);

        // 基本验证：确保返回的SseEmitter不为null
        Assertions.assertNotNull(emitter);

        // 可以添加短暂等待，让异步操作开始执行
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}

