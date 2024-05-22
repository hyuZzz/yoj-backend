package com.yuhi.yojbackendquestionservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.yuhi.yojbackendcommon.annotation.AuthCheck;
import com.yuhi.yojbackendcommon.common.BaseResponse;
import com.yuhi.yojbackendcommon.common.DeleteRequest;
import com.yuhi.yojbackendcommon.common.ErrorCode;
import com.yuhi.yojbackendcommon.common.ResultUtils;
import com.yuhi.yojbackendcommon.constant.UserConstant;
import com.yuhi.yojbackendcommon.exception.BusinessException;
import com.yuhi.yojbackendcommon.exception.ThrowUtils;
import com.yuhi.yojbackendmodel.model.dto.question.*;
import com.yuhi.yojbackendmodel.model.dto.questionsumbit.QuestionSubmitAddRequest;
import com.yuhi.yojbackendmodel.model.dto.questionsumbit.QuestionSubmitQueryRequest;
import com.yuhi.yojbackendmodel.model.entity.Question;
import com.yuhi.yojbackendmodel.model.entity.QuestionSubmit;
import com.yuhi.yojbackendmodel.model.entity.User;
import com.yuhi.yojbackendmodel.model.vo.QuestionSubmitVO;
import com.yuhi.yojbackendmodel.model.vo.QuestionVO;
import com.yuhi.yojbackendquestionservice.manage.RedissonLimiter;
import com.yuhi.yojbackendquestionservice.service.QuestionService;
import com.yuhi.yojbackendquestionservice.service.QuestionSubmitService;
import com.yuhi.yojbackendserviceclient.service.UserFeignClient;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 题目管理
 *
 * @author yuhi
 */
@RestController
@RequestMapping("/")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private RedissonLimiter redissonLimiter;

    private final static Gson GSON = new Gson();


    /**
     * 提交题目
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @ApiOperation(value = "添加题目")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        // 参数校验，确保请求参数 questionAddRequest 不为 null
        if (questionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 创建题目对象
        Question question = new Question();
        // 将 questionAddRequest 中的属性值复制到 question 对象中
        BeanUtils.copyProperties(questionAddRequest, question);
        // 处理标签
        List<String> tags = questionAddRequest.getTags();
        if (tags != null) {
            // 将标签转换为 JSON 字符串，并设置到 question 对象的 tags 属性中
            question.setTags(GSON.toJson(tags));
        }
        // 处理判断用例
        List<JudgeCase> judgeCase = questionAddRequest.getJudgeCase();
        if (judgeCase != null) {
            // 将判断配置转换为 JSON 字符串，并设置到 question 对象的 judgeConfig 属性中
            question.setJudgeCase(GSON.toJson(judgeCase));
        }
        // 处理判断配置
        JudgeConfig judgeConfig = questionAddRequest.getJudgeConfig();
        if (judgeConfig != null) {
            // 将判断配置转换为 JSON 字符串，并设置到 question 对象的 judgeConfig 属性中
            question.setJudgeConfig(GSON.toJson(judgeConfig));
        }
        // 对题目进行校验，确保题目的有效性
        questionService.validQuestion(question, true);
        // 获取登录用户信息，并将用户的 ID 设置到 question 对象的 userId 属性中
        User loginUser = userFeignClient.getLoginUser(request);
        question.setUserId(loginUser.getId());
        // 设置题目的初始值
        question.setFavourNum(0);
        question.setThumbNum(0);
        // 保存题目，并将保存结果保存在 result 变量中
        boolean result = questionService.save(question);
        // 检查保存结果，如果保存失败则抛出异常
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 获取新题目的 ID
        long newQuestionId = question.getId();
        // 构造成功的响应结果，将 newQuestionId 作为数据返回
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除题目
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @ApiOperation(value = "删除题目")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userFeignClient.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userFeignClient.isAdmin(user)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = questionService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "管理员更新信息")
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        List<String> tags = questionUpdateRequest.getTags();
        if (tags != null) {
            question.setTags(GSON.toJson(tags));
        }
        List<JudgeCase> judgeCase = questionUpdateRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(GSON.toJson(judgeCase));
        }
        JudgeConfig judgeConfig = questionUpdateRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(GSON.toJson(judgeConfig));
        }
        // 参数校验
        questionService.validQuestion(question, false);
        long id = questionUpdateRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = questionService.updateById(question);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取 (脱敏)
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    @ApiOperation(value = "获取题目信息")
    public BaseResponse<QuestionVO> getQuestionVoById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(questionService.getQuestionVO(question, request));
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @ApiOperation(value = "获取题目信息")
    public BaseResponse<Question> getQuestionById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 用户权限判断
        User loginUser = userFeignClient.getLoginUser(request);
        // 非本人或者管理员，不能获取到题目所有信息
        if (!question.getUserId().equals(loginUser.getId()) && userFeignClient.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
        }
        return ResultUtils.success(question);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    @ApiOperation(value = "分页获取题目列表")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 分页获取题目列表（仅管理员）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "管理员分页获取题目列表（所有信息）")
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                           HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        System.out.println("current:"+current);
        long size = questionQueryRequest.getPageSize();
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    @ApiOperation(value = "分页获取当前用户创建的题目列表")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        if (questionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 编辑（用户）
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @ApiOperation(value = "编辑题目")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        List<String> tags = questionEditRequest.getTags();
        if (tags != null) {
            question.setTags(GSON.toJson(tags));
        }
        List<JudgeCase> judgeCase = questionEditRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(GSON.toJson(judgeCase));
        }
        JudgeConfig judgeConfig = questionEditRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(GSON.toJson(judgeConfig));
        }
        // 参数校验
        questionService.validQuestion(question, false);
        User loginUser = userFeignClient.getLoginUser(request);
        long id = questionEditRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !userFeignClient.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = questionService.updateById(question);
        return ResultUtils.success(result);
    }

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param request
     * @return 提交记录 id
     */
    @PostMapping("/question_submit/do")
    @ApiOperation(value = "题目提交")
    public BaseResponse<Long> doQuestionSubmit(@RequestBody QuestionSubmitAddRequest questionSubmitAddRequest,
                                               HttpServletRequest request) {
        if (questionSubmitAddRequest == null || questionSubmitAddRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能提交
        final User loginUser = userFeignClient.getLoginUser(request);
        // 限流
        boolean rateLimit = redissonLimiter.doRateLimit(loginUser.getId().toString());
        if (!rateLimit) {
            return ResultUtils.error(ErrorCode.TOO_MANY_REQUEST, "提交过于频繁,请稍后重试");
        }
        long questionSubmitId = questionSubmitService.doQuestionSubmit(questionSubmitAddRequest, loginUser);
        return ResultUtils.success(questionSubmitId);
    }

    /**
     * 分页获取题目提交列表（除了管理员外，其他普通用户只能看到非用户答案、提交的代码等公开信息）
     *
     * @param questionSubmitQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/question_submit/list/page")
    @ApiOperation(value = "分页获取题目提交列表")
    public BaseResponse<Page<QuestionSubmitVO>> listQuestionSubmitByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
                                                                         HttpServletRequest request) {
        long current = questionSubmitQueryRequest.getCurrent();
        long pageSize = questionSubmitQueryRequest.getPageSize();

        // 从数据库中查询到原始的题目提交信息
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(current, pageSize),
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        final User loginUer = userFeignClient.getLoginUser(request);
        // 返回脱敏信息
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVOPage(questionSubmitPage, loginUer));
    }
}
