package com.yuhi.yojbackendjudgeservice.judge.service.impl;

import cn.hutool.json.JSONUtil;
import com.yuhi.yojbackendcommon.common.ErrorCode;
import com.yuhi.yojbackendcommon.exception.BusinessException;
import com.yuhi.yojbackendjudgeservice.judge.JudgeManager;
import com.yuhi.yojbackendjudgeservice.judge.codesandbox.CodeSandBox;
import com.yuhi.yojbackendjudgeservice.judge.codesandbox.CodeSandBoxProxy;
import com.yuhi.yojbackendjudgeservice.judge.codesandbox.CodeSandboxFactory;
import com.yuhi.yojbackendjudgeservice.judge.service.JudgeService;
import com.yuhi.yojbackendjudgeservice.judge.strategy.JudgeContext;
import com.yuhi.yojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.yuhi.yojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.yuhi.yojbackendmodel.model.codesandbox.JudgeInfo;
import com.yuhi.yojbackendmodel.model.dto.question.JudgeCase;
import com.yuhi.yojbackendmodel.model.entity.Question;
import com.yuhi.yojbackendmodel.model.entity.QuestionSubmit;
import com.yuhi.yojbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.yuhi.yojbackendserviceclient.service.QuestionFeignClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yuhi
 * @createTime 2023/10/30 星期三 12:09
 * 判题服务实现类
 */
@Service
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionFeignClient questionFeignClient;

    @Resource
    private JudgeManager judgeManager;

    @Value("${codesandbox.type:remote}")
    private String judgeType;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        // 1、传入题目的提交 id，获取到对应的题目、提交信息（包含代码、编程语言等）
        QuestionSubmit questionSubmit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        // 通过提交的信息中的题目id 获取到题目的全部信息
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionFeignClient.getQuestionById(questionId);
        if (questionId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        // 2、如果题目提交状态不为等待中
        if (!questionSubmit.getSubmitState().equals(QuestionSubmitStatusEnum.WAITING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在判题中");
        }
        // 更改判题（题目提交）的状态为 “判题中”，防止重复执行，也能让用户即时看到状态
        QuestionSubmit updateQuestionSubmit = new QuestionSubmit();
        updateQuestionSubmit.setId(questionSubmitId);
        updateQuestionSubmit.setSubmitState(QuestionSubmitStatusEnum.RUNNING.getValue());
        boolean updateState = questionFeignClient.updateQuestionSubmitById(updateQuestionSubmit);
        if (!updateState) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新失败");
        }


        // 3、调用代码沙箱，获取到执行结果
        CodeSandBox codeSandbox = CodeSandboxFactory.newInstance("remote");
        codeSandbox = new CodeSandBoxProxy(codeSandbox);
        String submitLanguage = questionSubmit.getSubmitLanguage();
        String submitCode = questionSubmit.getSubmitCode();
        // 获取输入用例
        String judgeCaseStr = question.getJudgeCase();
        List<JudgeCase> judgeCasesList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
        // 通过Lambda表达式获取到每个题目的输入用例
        List<String> inputList = judgeCasesList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        // 调用沙箱
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(submitCode)
                .language(submitLanguage)
                .inputList(inputList)
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        List<String> outputList = executeCodeResponse.getOutputList();

        // 4、根据沙箱的执行结果，设置题目的判题状态和信息
        JudgeContext judgeContext = new JudgeContext();
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        judgeContext.setInputList(inputList);
        judgeContext.setOutputList(outputList);
        judgeContext.setJudgeCaseList(judgeCasesList);
        judgeContext.setQuestion(question);
        judgeContext.setQuestionSubmit(questionSubmit);

        JudgeInfo judgeInfo;
        if (judgeContext.getJudgeInfo() != null) {//如果返回输出结果编译正确则进入死信队列
            // 进行判题
            judgeInfo = judgeManager.doJudge(judgeContext);
            // 6、修改判题结果
            updateQuestionSubmit = new QuestionSubmit();
            updateQuestionSubmit.setId(questionSubmitId);
            if (judgeContext.getJudgeInfo().getTime()==0) {//超时和内存过大会返回time：0
                updateQuestionSubmit.setSubmitState(QuestionSubmitStatusEnum.FAILED1.getValue());
            } else {
                updateQuestionSubmit.setSubmitState(QuestionSubmitStatusEnum.SUCCEED.getValue());
            }
//            updateQuestionSubmit.setSubmitState(QuestionSubmitStatusEnum.SUCCEED.getValue());
            updateQuestionSubmit.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        } else {//无则返回编译失败信息
            updateQuestionSubmit.setSubmitState(QuestionSubmitStatusEnum.FAILED.getValue());
        }
        updateState = questionFeignClient.updateQuestionSubmitById(updateQuestionSubmit);

        if (!updateState) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新失败");
        }
        // 再次查询数据库，返回最新提交信息
        QuestionSubmit questionSubmitResult = questionFeignClient.getQuestionSubmitById(questionId);
        return questionSubmitResult;
    }
}
