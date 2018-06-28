package com.jh.jsuk.mq;


import com.jh.jsuk.entity.Mq;
import com.jh.jsuk.entity.dto.MessageDTO;
import com.jh.jsuk.envm.MqStatus;
import com.jh.jsuk.service.MqService;
import com.jh.jsuk.utils.DisJPushUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;

import static com.jh.jsuk.conf.QueueConfig.QUEUE_PUSH_MESSAGE;

@Slf4j
@Component
@RabbitListener(queues = QUEUE_PUSH_MESSAGE)
public class DjsMessageConsumer {

    @Autowired
    MqService mqService;

    @RabbitHandler
    public void process(MessageDTO data) throws Exception {
        Mq mq = mqService.selectById(data.getId());
        if (mq == null || mq.isConsumed())
            return;
        try {
            DisJPushUtils.pushMsg2(data.getAlias(), data.getContent(), data.getTitle(), new HashMap<>());
            mq.setConsumeTime(new Date());
            mq.setStatus(MqStatus.CONSUME.getKey());
            mq.updateById();
        } catch (Exception e) {
            log.error(e.getMessage());
            mq.setFailureReason(e.getMessage());
            mq.updateById();
            throw e;
        }

    }

}