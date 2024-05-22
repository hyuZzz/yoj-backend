package com.yuhi.yojbackenduserservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author yuhi
 */
@SpringBootApplication()
@MapperScan("com.yuhi.yojbackenduserservice.mapper")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan("com.yuhi")
@EnableDiscoveryClient
@EnableFeignClients(basePackages ={"com.yuhi.yojbackendserviceclient.service"})
public class YojBackendUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(YojBackendUserServiceApplication.class, args);
    }

}
