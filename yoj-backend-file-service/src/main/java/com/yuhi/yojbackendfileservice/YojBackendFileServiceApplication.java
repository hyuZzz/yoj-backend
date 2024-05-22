package com.yuhi.yojbackendfileservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan("com.yuhi")
@EnableDiscoveryClient
@EnableFeignClients(basePackages ={"com.yuhi.yojbackendserviceclient.service"})
public class YojBackendFileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(YojBackendFileServiceApplication.class, args);
    }

}
