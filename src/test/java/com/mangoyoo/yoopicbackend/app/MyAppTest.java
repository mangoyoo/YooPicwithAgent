package com.mangoyoo.yoopicbackend.app;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.util.AssertionErrors.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class MyAppTest {

    @Resource
    private MyApp myApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "请你帮我解读一下这张照片";
        String image = "http://www.yoodns.yoopic.space/public/1920742554046758914/2025-05-09_cxpM3I3bvbj9ciT8.webp";

        Flux<String> responseFlux = myApp.doChatWithImageByStream(message, image, chatId);

        // 收集所有响应并打印
        List<String> responses = responseFlux
                .doOnNext(response -> System.out.println("收到响应: " + response))
                .collectList()
                .block(); // 阻塞等待完成

//        // 验证结果
//        assertNotNull(responses);
//        assertFalse(responses.isEmpty());
        System.out.println("总共收到 " + responses.size() + " 条响应");
    }
    @Test
    void testChat2() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "nihao,wo shi super man";


        Flux<String> responseFlux = myApp.doChatByStream(message,chatId);

        // 收集所有响应并打印
        List<String> responses = responseFlux
                .doOnNext(response -> System.out.println("收到响应: " + response))
                .collectList()
                .block(); // 阻塞等待完成

//        // 验证结果
//        assertNotNull(responses);
//        assertFalse(responses.isEmpty());
        System.out.println("总共收到 " + responses.size() + " 条响应");
    }
    @Test
    public void test3() {
        // 准备测试数据
        String chatId = UUID.randomUUID().toString();
        String message = "请你帮我解读一下这张照片";
        String imageUrl = "http://www.yoodns.yoopic.space/public/1920742554046758914/2025-05-09_cxpM3I3bvbj9ciT8.webp";

        // 执行被测试方法
        String result = myApp.doChatWithImageSync(message, imageUrl, chatId);

        // 验证结果
        assertNotNull(result, "返回结果不应为空");
//        assertFalse(result.isEmpty(), "返回结果不应为空字符串");

        // 验证不是错误信息（如果返回结果包含"抱歉，处理图片时发生错误"说明有异常）
//        assertFalse(result.startsWith("抱歉，处理图片时发生错误"), "不应返回错误信息");

        // 打印结果便于查看
        log.info("同步图片对话测试结果: {}", result);
        log.info("使用的chatId: {}", chatId);
    }

    @Test
    void doChatWithTools() {
//        // 测试联网搜索问题的答案
//        testMessage("周末想带女朋友去上海约会，推荐几个适合情侣的小众打卡地？");
//
//        // 测试网页抓取：恋爱案例分析
//        testMessage("最近和对象吵架了，看看编程导航网站（codefather.cn）的其他情侣是怎么解决矛盾的？");

        // 测试资源下载：图片下载
        testMessage("直接下载一张适合做手机壁纸的星空情侣图片为文件");

//        // 测试终端操作：执行代码
//        testMessage("执行 Python3 脚本来生成数据分析报告");
//
//        // 测试文件操作：保存用户档案
//        testMessage("保存我的恋爱档案为文件");

        // 测试 PDF 生成
//        testMessage("生成一份‘七夕约会计划’PDF，包含餐厅预订、活动流程和礼物清单");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = myApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        // 测试地图 MCP
//        String message = "我的另一半居住在广州大学城，请帮我找到 10 公里内合适的约会地点";
        String message = "帮我搜索有关大海的图片";
        String answer =  myApp.doChatWithMcp(message, chatId);
    }



}
