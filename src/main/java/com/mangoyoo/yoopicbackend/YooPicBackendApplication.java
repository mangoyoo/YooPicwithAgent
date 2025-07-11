package com.mangoyoo.yoopicbackend;
import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.mangoyoo.yoopicbackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync
public class YooPicBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YooPicBackendApplication.class, args);
    }
//test
}
