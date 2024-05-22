package com.yuhi.yojbackendjudgeservice.judge;

import com.yuhi.yojbackendjudgeservice.judge.strategy.DefaultJudgeStrategy;
import com.yuhi.yojbackendjudgeservice.judge.strategy.JavaLanguageJudgeStrategy;
import com.yuhi.yojbackendjudgeservice.judge.strategy.JudgeContext;
import com.yuhi.yojbackendjudgeservice.judge.strategy.JudgeStrategy;
import com.yuhi.yojbackendmodel.model.codesandbox.JudgeInfo;
import com.yuhi.yojbackendmodel.model.entity.QuestionSubmit;
import org.springframework.stereotype.Service;

/**
 * 判题管理（简化调用）
 *
 * @author yuhi
 */
@Service
public class JudgeManager {

    /**
     * 执行判题
     *
     * @param judgeContext
     * @return
     */
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        String language = questionSubmit.getSubmitLanguage();
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        if ("java".equals(language)) {
            judgeStrategy = new JavaLanguageJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }
}