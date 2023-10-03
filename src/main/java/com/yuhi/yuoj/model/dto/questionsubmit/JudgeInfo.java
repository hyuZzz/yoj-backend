package com.yuhi.yuoj.model.dto.questionsubmit;

import lombok.Data;

/**
 * 题目配置
 */
@Data
public class JudgeInfo {

    /**
     * 时间限制（ms）
     */
    private Long timeLimit;

    /**
     * 内存限制（KB）
     */
    private Long memoryLimit;

    /**
     * 堆栈限制（KB）
     */
    private Long stackLimit;
}
