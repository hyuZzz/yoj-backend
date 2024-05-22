package com.yuhi.yojbackendjudgeservice.mq;

import com.rabbitmq.client.Channel;
import com.yuhi.yojbackendcommon.common.ErrorCode;
import com.yuhi.yojbackendcommon.exception.BusinessException;
import com.yuhi.yojbackendjudgeservice.judge.service.JudgeService;
import com.yuhi.yojbackendmodel.model.entity.Question;
import com.yuhi.yojbackendmodel.model.entity.QuestionSubmit;
import com.yuhi.yojbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.yuhi.yojbackendserviceclient.service.QuestionFeignClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

import static com.yuhi.yojbackendcommon.constant.MqConstant.CODE_QUEUE;

/**
 * Mq 消费者
 *
 * @author yuuu
 * CreateTime 2023/11/24 15:53
 */
@Component
@Slf4j
public class CodeMqConsumer {

    @Resource
    private JudgeService judgeService;

    @Resource
    private QuestionFeignClient questionFeignClient;


    /**
     * 指定程序监听的消息队列和确认机制
     *
     * @param message
     * @param channel
     * @param deliveryTag
     */
    @SneakyThrows
    @RabbitListener(queues = {CODE_QUEUE}, ackMode = "MANUAL", concurrency = "2")//queues为指定监听的队列名称，ackmode为消息确认方式，concurrency为指定并发消费者的数量
    private void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("接收到消息 ： {}", message);
        long questionSubmitId = Long.parseLong(message);

        if (message == null) {
            // 消息为空，则拒绝消息（不重试），进入死信队列
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NULL_ERROR, "消息为空");
        }
        try {
            judgeService.doJudge(questionSubmitId);
            QuestionSubmit questionSubmit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
            if (!questionSubmit.getSubmitState().equals(QuestionSubmitStatusEnum.SUCCEED.getValue())) {
                channel.basicNack(deliveryTag, false, false);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "判题失败");
            }
            log.info("新提交的信息：" + questionSubmit);
            // 设置通过数
            Long questionId = questionSubmit.getQuestionId();
            log.info("题目:" + questionId);
            Question question = questionFeignClient.getQuestionById(questionId);
            Integer acceptedNum = question.getAcceptedNum();
            Question updateQuestion = new Question();
            synchronized (question.getAcceptedNum()) {
                if (questionSubmit.getJudgeInfo().contains("成功")) {
                    acceptedNum = acceptedNum + 1;
                }
                updateQuestion.setId(questionId);
                updateQuestion.setAcceptedNum(acceptedNum);
                boolean save = questionFeignClient.updateQuestion(updateQuestion);
                if (!save) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存数据失败");
                }
            }
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            // 消息为空，则拒绝消息，进入死信队列
            channel.basicNack(deliveryTag, false, false);
            throw new RuntimeException(e);
        }
    }
}
