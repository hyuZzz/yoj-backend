package com.yuhi.yuoj.model.dto.questionsubmit;

import com.yuhi.yuoj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 创建请求
 *
 * 
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionSubmitAddRequest extends PageRequest implements Serializable {

    /**
     * 编程语言
     */
    private String language;

    /**
     * 用户代码
     */
    private String code;



    /**
     * 题目 id
     */
    private Long questionId;



    private static final long serialVersionUID = 1L;
}