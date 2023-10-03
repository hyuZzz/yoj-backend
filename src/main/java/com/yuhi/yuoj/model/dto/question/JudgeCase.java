package com.yuhi.yuoj.model.dto.question;

import lombok.Data;//使用lombok自动生成Get,Set方法

/**
 * 题目用例
 */
@Data
public class JudgeCase {

    /**
     * 输入用例
     */
    private String input;

    /**
     * 输出用例
     */
    private String output;
}
