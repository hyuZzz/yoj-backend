package com.yuhi.yojbackendjudgeservice.controller.inner;

import com.yuhi.yojbackendjudgeservice.judge.service.JudgeService;
import com.yuhi.yojbackendmodel.model.entity.QuestionSubmit;
import com.yuhi.yojbackendserviceclient.service.JudgeFeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用户内部服务（仅内部调用）
 *
 * @author yuhi 2023/9/6 15:39
 */
@RestController
@RequestMapping("/inner")
public class JudgeInnerController implements JudgeFeignClient {
    @Resource
    private JudgeService judgeService;

    /**
     * 判题
     *
     * @param questionSubmitId
     * @return
     */
    @PostMapping("/do")
    @Override
    public QuestionSubmit doJudge(@RequestParam("questionSubmitId") long questionSubmitId) {
        return judgeService.doJudge(questionSubmitId);
    }

}
