package com.yuhi.yuoj.judge;

import com.yuhi.yuoj.judge.strategy.DefaultJudgeStrategy;
import com.yuhi.yuoj.judge.strategy.JavaLanguageJudgeStrategy;
import com.yuhi.yuoj.judge.strategy.JudgeContext;
import com.yuhi.yuoj.judge.strategy.JudgeStrategy;
import com.yuhi.yuoj.model.dto.questionsubmit.JudgeInfo;
import com.yuhi.yuoj.model.entity.QuestionSubmit;
import org.springframework.stereotype.Service;

/**
 * 判题管理  （简化调用）
 */
@Service
public class JudgeManager {

    /**
     * 执行判题
     *
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext) {
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        String language = questionSubmit.getLanguage();
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        if ("java".equals(language)) {
            judgeStrategy = new JavaLanguageJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }

}
