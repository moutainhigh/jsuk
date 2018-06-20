package com.jh.jsuk.mq;

import com.jushang.entity.Order;
import com.jushang.entity.dto.UserOrderDTO;
import com.jushang.service.OrderService;
import com.jushang.utils.DisJPushUtils;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = "testQueue")
public class Consumer {

    @Autowired
    private OrderService orderService;

    @RabbitHandler
    public void process(UserOrderDTO data) {
        Integer orderId = data.getOrderId();
        Order order = orderService.selectById(orderId);
        Integer userId = data.getUserId();
        synchronized (this) {
            if (order != null && order.getDistributionUserId() == null && order.getStatus() == 2) {
                order.setDistributionUserId(userId);
                order.setStatus(3);
                order.updateById();
                //System.out.println(orderId+"========="+userId);
                DisJPushUtils.pushMsg(userId + "", "抢单成功！", "", null);
                //发送成功推送
            } else {
                DisJPushUtils.pushMsg(userId + "", "很抱歉慢了一步，订单已被其他骑手抢走", "", null);
                //发送失败推送
            }
        }
    }

}
