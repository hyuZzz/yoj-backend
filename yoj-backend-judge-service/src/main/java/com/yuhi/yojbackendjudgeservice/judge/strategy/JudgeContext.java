package com.yuhi.yojbackendjudgeservice.judge.strategy;

import com.yuhi.yojbackendmodel.model.codesandbox.JudgeInfo;
import com.yuhi.yojbackendmodel.model.dto.question.JudgeCase;
import com.yuhi.yojbackendmodel.model.entity.Question;
import com.yuhi.yojbackendmodel.model.entity.QuestionSubmit;
import lombok.Data;

import java.util.List;

/**
 * 上下文（用于定义在策略中传递的参数）
 * @author yuhi
 */
@Data
public class JudgeContext {

    private JudgeInfo judgeInfo;

    private List<String> inputList;

    private List<String> outputList;

    private List<JudgeCase> judgeCaseList;

    private Question question;

    private QuestionSubmit questionSubmit;

}
