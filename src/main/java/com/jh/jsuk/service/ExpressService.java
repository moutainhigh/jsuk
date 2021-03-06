package com.jh.jsuk.service;

import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.IService;
import com.jh.jsuk.entity.Express;
import com.jh.jsuk.entity.UserOrder;
import com.jh.jsuk.entity.vo.ExpressVo2;
import com.jh.jsuk.envm.ExpressStatus;
import com.jh.jsuk.envm.UserType;

import java.util.List;

/**
 * <p>
 * 快递跑腿 服务类
 * </p>
 *
 * @author lpf
 * @since 2018-06-20
 */
public interface ExpressService extends IService<Express> {

    Page getExpressListBy(Page page, Wrapper wrapper, Integer status, Integer type, Integer userId);

    Page getDeliverList(Page page, Wrapper ew, String status, Integer type, Integer userId, String lng, String lat, Integer cityId) throws Exception;

    Page listPage(Page page, ExpressStatus expressStatus, List<String> dates, String kw);

    int statusCount(ExpressStatus cancel, Integer shopId, UserType userType, Integer userId);

    Page listOrderByDistributionId(Page page, Integer distributionId);

    ExpressVo2 detail(Integer expressId);

    /**
     * 骑手抢单
     *
     * @param userId
     * @param expressId
     * @return
     */
    boolean deliverRobbingOrder(Integer userId, Integer expressId);

    /**
     * 快递跑腿 余额支付
     */
    void balancePay(String orderId, Integer userId) throws Exception;

    /**
     * 创建已经支付了的订单
     *
     * @param order
     * @param userId
     */
    void createCityDistributionPayed(UserOrder order, Integer userId, Integer cityId);

    /**
     * 获取当前城市可接订单量
     * @param cityId
     * @return
     */
    Integer getOrderName(Integer cityId);
}
