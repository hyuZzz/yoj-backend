package com.yuhi.yojbackendjudgeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

import static com.yuhi.yojbackendjudgeservice.mq.CodeMqInitMain.doInitCodeMq;

/**
 * @author yuhi
 */
@SpringBootApplication()
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan("com.yuhi")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.yuhi.yojbackendserviceclient.service"})
public class YojBackendJudgeServiceApplication {

    public static void main(String[] args) {
        // 初始化消息队列
        doInitCodeMq();
        SpringApplication.run(YojBackendJudgeServiceApplication.class, args);
    }
}
