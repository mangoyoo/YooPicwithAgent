package com.mangoyoo.yoopicbackend.Manus;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MyManusTest {

    @Resource
    private MyManus myManus;

    @Test
    void run() {
        String userPrompt = """  
               帮我在本站找2张标签为表情包的图片，返回url给我   
                """;
        String answer = myManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}

