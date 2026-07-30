package com.ljc.chaocommunity;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.ljc.chaocommunity.mapper") //扫描mapper接口包
public class ChaoCommunityApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChaoCommunityApplication.class, args);
    }

}
