package com.yuhi.yojbackendjudgeservice.judge.strategy;


import com.yuhi.yojbackendmodel.model.codesandbox.JudgeInfo;

/**
 * 判题策略
 * @author yuhi
 */
public interface JudgeStrategy {

    /**
     * 执行判题
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext);
}