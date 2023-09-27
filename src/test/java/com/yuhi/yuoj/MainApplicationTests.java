package com.yuhi.yuoj;

import com.yuhi.yuoj.config.WxOpenConfig;

import javax.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 主类测试
 *
 */
@SpringBootTest
class MainApplicationTests {
    //再再再次测试git上传

    //启动测试
    @Resource
    private WxOpenConfig wxOpenConfig;

    @Test
    void contextLoads() {
        System.out.println(wxOpenConfig);
    }

}
