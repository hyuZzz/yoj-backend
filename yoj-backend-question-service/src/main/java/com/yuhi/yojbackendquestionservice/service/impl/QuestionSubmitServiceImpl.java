package com.yuhi.yojbackendquestionservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuhi.yojbackendcommon.common.ErrorCode;
import com.yuhi.yojbackendcommon.constant.CommonConstant;
import com.yuhi.yojbackendcommon.exception.BusinessException;
import com.yuhi.yojbackendcommon.exception.ThrowUtils;
import com.yuhi.yojbackendcommon.utils.SqlUtils;
import com.yuhi.yojbackendmodel.model.dto.questionsumbit.QuestionSubmitAddRequest;
import com.yuhi.yojbackendmodel.model.dto.questionsumbit.QuestionSubmitQueryRequest;
import com.yuhi.yojbackendmodel.model.entity.Question;
import com.yuhi.yojbackendmodel.model.entity.QuestionSubmit;
import com.yuhi.yojbackendmodel.model.entity.User;
import com.yuhi.yojbackendmodel.model.enums.QuestionSubmitLanguageEnum;
import com.yuhi.yojbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.yuhi.yojbackendmodel.model.vo.QuestionSubmitVO;
import com.yuhi.yojbackendmodel.model.vo.QuestionVO;
import com.yuhi.yojbackendmodel.model.vo.UserVO;
import com.yuhi.yojbackendquestionservice.mapper.QuestionSubmitMapper;
import com.yuhi.yojbackendquestionservice.mq.CodeMqProducer;
import com.yuhi.yojbackendquestionservice.service.QuestionService;
import com.yuhi.yojbackendquestionservice.service.QuestionSubmitService;
import com.yuhi.yojbackendserviceclient.service.JudgeFeignClient;
import com.yuhi.yojbackendserviceclient.service.UserFeignClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static com.yuhi.yojbackendcommon.constant.MqConstant.CODE_EXCHANGE_NAME;
import static com.yuhi.yojbackendcommon.constant.MqConstant.CODE_ROUTING_KEY;

/**
 * @author yuhi
 * @description 针对表【question_submit(题目提交)】的数据库操作Service实现
 * @createDate 2023-08-25 17:33:25
 */
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
        implements QuestionSubmitService {

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private QuestionService questionService;

    @Resource
    @Lazy
    private JudgeFeignClient judgeFeignClient;

    @Resource
    private CodeMqProducer codeMqProducer;

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest 请求包装类
     * @param loginUser                登录用户
     * @return 提交题目 id
     */
    @Override
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        Long questionId = questionSubmitAddRequest.getQuestionId();
        String submitLanguage = questionSubmitAddRequest.getSubmitLanguage();
        String submitCode = questionSubmitAddRequest.getSubmitCode();
        // 校验编程语言是否正确
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(submitLanguage);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }
        // 判断实体是否存在，根据类别获取实体
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 设置提交数
        Integer submitNum = question.getSubmitNum();
        Question updateQuestion = new Question();
        synchronized (question.getSubmitNum()) {
            submitNum = submitNum + 1;
            updateQuestion.setId(questionId);
            updateQuestion.setSubmitNum(submitNum);
            boolean save = questionService.updateById(updateQuestion);
            if (!save) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据保存失败");
            }
        }
        // 是否已提交题目
        long userId = loginUser.getId();
        // 每个用户串行提交题目
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(userId);
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setSubmitCode(submitCode);
        questionSubmit.setSubmitLanguage(submitLanguage);
        // 设置初始状态
        questionSubmit.setSubmitState(QuestionSubmitStatusEnum.WAITING.getValue());
        questionSubmit.setJudgeInfo("{}");
        boolean save = this.save(questionSubmit);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "数据保存失败");

        Long questionSubmitId = questionSubmit.getId();
        // 生产者发送消息
        codeMqProducer.sendMessage(CODE_EXCHANGE_NAME, CODE_ROUTING_KEY, String.valueOf(questionSubmitId));
        return questionSubmitId;
    }


    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象）
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {


        Long questionId = questionSubmitQueryRequest.getQuestionId();
        String submitLanguage = questionSubmitQueryRequest.getSubmitLanguage();
        Integer submitState = questionSubmitQueryRequest.getSubmitState();
        Long userId = questionSubmitQueryRequest.getUserId();
        String sortField = questionSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();

        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }

        // 拼接查询条件
        queryWrapper.eq(ObjectUtils.isNotEmpty(submitLanguage), "submitLanguage", submitLanguage);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(QuestionSubmitStatusEnum.getEnumByValue(submitState) != null, "submitState", submitState);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取查询封装类（单个）
     *
     * @param questionSubmit
     * @param loginUser
     * @return
     */
    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser) {
        QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        // 脱敏：仅本人和管理员能看见自己（提交 userId 和登录用户 id 不同）提交的代码
        long userId = loginUser.getId();
        // 处理脱敏
        if (userId != questionSubmit.getUserId() && !userFeignClient.isAdmin(loginUser)) {
            questionSubmitVO.setSubmitCode(null);
        }
        QuestionVO questionVO = QuestionVO.objToVo(questionService.getById(questionSubmitVO.getQuestionId()));
        // 设置题目信息
        Long submitUserId=questionSubmitVO.getUserId();
        User user=userFeignClient.getById(submitUserId);
        UserVO userVO =new UserVO();
        userVO.setUserName(user.getUserName());
        questionSubmitVO.setQuestionVO(questionVO);
        questionSubmitVO.setUserVO(userVO);
        System.out.println("user.getUserName()123123123"+user.getUserName());
        System.out.println("questionVO123123123"+questionVO);
        return questionSubmitVO;
    }

//    /**
//     * 获取查询脱敏信息
//     *
//     * @param questionSubmitPage 题目提交分页
//     * @param loginUser          直接获取到用户信息，减少查询数据库
//     * @return
//     */
//    @Override
//    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser) {
//        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
//        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
//        if (CollectionUtils.isEmpty(questionSubmitList)) {
//            return questionSubmitVOPage;
//        }
//        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream()
//                .map(questionSubmit -> getQuestionSubmitVO(questionSubmit, loginUser))
//                .collect(Collectors.toList());
//        questionSubmitVOPage.setRecords(questionSubmitVOList);
//        return questionSubmitVOPage;
//    }
    /**
     * 获取查询脱敏信息的分页数据
     *
     * @param questionSubmitPage 题目提交分页对象
     * @param loginUser          当前登录用户信息，用于脱敏处理
     * @return 包含脱敏信息的分页数据对象（QuestionSubmitVO）
     */
    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser) {
        // 从输入的题目提交分页对象中获取题目提交记录列表
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        // 创建一个新的分页对象，保留原始分页信息（当前页码、每页大小、总记录数）
        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        // 如果题目提交记录列表为空，直接返回空的分页对象
        if (CollectionUtils.isEmpty(questionSubmitList)) {
            return questionSubmitVOPage;
        }
        // 使用流式操作将每个题目提交记录映射为对应的脱敏信息（QuestionSubmitVO）
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream()
                .map(questionSubmit -> getQuestionSubmitVO(questionSubmit, loginUser))
                .collect(Collectors.toList());
        // 将映射得到的脱敏信息列表设置为新的分页对象的记录列表
        questionSubmitVOPage.setRecords(questionSubmitVOList);
        // 返回包含脱敏信息的分页数据对象
        return questionSubmitVOPage;
    }

}




