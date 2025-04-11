package com.mangoyoo.yoopicbackend.tools;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
class TestTools {

    @Autowired
    private HotSearchTool hotSearchTool;

    @Test
    void run() {
        // 你的测试代码
//        hotSearchTool.getHotNewsContent();
    }
}

