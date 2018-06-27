package com.jh.jsuk.service;

import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.IService;
import com.jh.jsuk.entity.UserOrder;
import com.jh.jsuk.entity.vo.UserOrderVo;

import java.util.List;

/**
 * <p>
 * 订单 服务类
 * </p>
 *
 * @author lpf
 * @since 2018-06-20
 */
public interface UserOrderService extends IService<UserOrder> {

    List<UserOrderVo> findVoByPage(Page page, Wrapper wrapper);

    Page<UserOrderVo> findVoPage(Page page, Wrapper wrapper);

    UserOrderVo findVoById(Integer id);

    void returnStock(Integer orderId);

    void remindingOrderTaking();

    Page getOrderByUserId(Page page, Wrapper wrapper, Integer userId, Integer status, String goodsName);
}
