package com.jh.jsuk.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.SqlHelper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.github.tj123.common.RedisUtils;
import com.jh.jsuk.conf.RedisKeys;
import com.jh.jsuk.dao.UserOrderDao;
import com.jh.jsuk.dao.UserOrderGoodsDao;
import com.jh.jsuk.entity.*;
import com.jh.jsuk.entity.dto.ShopSubmitOrderDto;
import com.jh.jsuk.entity.dto.ShopSubmitOrderGoodsDto;
import com.jh.jsuk.entity.dto.SubmitOrderDto;
import com.jh.jsuk.entity.info.UserRemainderInfo;
import com.jh.jsuk.entity.vo.*;
import com.jh.jsuk.envm.*;
import com.jh.jsuk.exception.MessageException;
import com.jh.jsuk.service.*;
import com.jh.jsuk.service.UserOrderService;
import com.jh.jsuk.utils.Date2;
import com.jh.jsuk.utils.EnumUitl;
import com.jh.jsuk.utils.ShopJPushUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * <p>
 * 订单 服务实现类
 * </p>
 *
 * @author lpf
 * @since 2018-06-20
 */
@Slf4j
@Service
public class UserOrderServiceImpl extends ServiceImpl<UserOrderDao, UserOrder> implements UserOrderService {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private UserOrderGoodsDao orderGoodsDao;
    @Autowired
    private UserService userService;
    @Autowired
    private ShopGoodsService shopGoodsService;
    @Autowired
    private UserRemainderService userRemainderService;
    @Autowired
    private ShopGoodsSizeService shopGoodsSizeService;
    @Autowired
    private UserOrderGoodsService userOrderGoodsService;
    @Autowired
    private ShopUserService shopUserService;
    @Autowired
    private UserOrderService userOrderService;
    @Autowired
    RedisUtils redisUtils;
    @Autowired
    private UserAddressService userAddressService;

    @Autowired
    private CouponService couponService;
    @Autowired
    private UserIntegralService userIntegralService;
    @Autowired
    private IntegralRuleService integralRuleService;
    @Autowired
    private ShopGoodsFullReduceService shopGoodsFullReduceService;
    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private ShopMoneyService shopMoneyService;
    @Autowired
    private ShopSetService shopSetService;//是否包邮

    @Override
    public int statusCount(OrderStatus orderStatus, Integer shopId) {
        EntityWrapper<UserOrder> wrapper = new EntityWrapper<>();
        if (orderStatus != null) {
            wrapper.eq(UserOrder.STATUS, orderStatus.getKey());
        }
        if (shopId != null) {
            wrapper.eq(UserOrder.SHOP_ID, shopId);
        }
//        wrapper.ne(UserOrder.IS_USER_DEL, 1);
        return selectCount(wrapper);
    }

    @Override
    public List<UserOrderVo> findVoByPage(Page page, Wrapper wrapper) {
        return baseMapper.findVoByPage(page, wrapper);
    }

    @Override
    public Page<UserOrderVo> findVoPage(Page page, Wrapper wrapper) {
        SqlHelper.fillWrapper(page, wrapper);
        page.setRecords(this.baseMapper.findVoByPage(page, wrapper));
        return page;
    }

    @Override
    public UserOrderVo findVoById(Integer id) {
        return baseMapper.findVoById(id);
    }

    @Override
    public void returnStock(Integer orderId) {
        try {
            List<UserOrderGoods> goodsList = orderGoodsDao
                .selectList(new EntityWrapper<UserOrderGoods>()
                    .eq("order_id", orderId));
            for (UserOrderGoods goods : goodsList) {
                shopGoodsService.returnStock(goods.getGoodsId(), goods.getNum());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void remindingOrderTaking() {
        List<UserOrder> orders = this.selectList(new EntityWrapper<UserOrder>()
            .eq("status", 1)
            .eq("is_unsubscribe", 0));
        for (UserOrder order : orders) {
            try {
                Integer shopId = order.getShopId();
                ShopUser shopUser = shopUserService.selectOne(new EntityWrapper<ShopUser>().eq("shop_id", shopId));
                ShopJPushUtils.pushMsgMusic(shopUser.getId() + "", "您有新的订单请注意接单", "", null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 用户端
     * @param page
     * @param wrapper
     * @param userId
     * @param status
     * @param goodsName
     * @return
     */
    @Override
    public Page getOrderByUserId(Page page, Wrapper wrapper, Integer userId, Integer status, String goodsName) {
        if (null == status) {
            if (goodsName != null) {
                page = userOrderService.selectPage(page, new EntityWrapper<UserOrder>().eq(UserOrder.USER_ID, userId)
                    .like(UserOrder.GOODS_NAME, goodsName).orderBy(true, UserOrder.UPDATE_TIME, false)
                    .where("not is_user_del"));
            } else {
                page = userOrderService.selectPage(page, new EntityWrapper<UserOrder>().eq(UserOrder.USER_ID, userId)
                    .orderBy(true, UserOrder.UPDATE_TIME, false)
                    .where("not is_user_del"));
            }
        }else if(status == 8){
            //待评价
            if (goodsName != null) {
                page = userOrderService.selectPage(page, new EntityWrapper<UserOrder>().eq(UserOrder.USER_ID, userId)
                    .eq(UserOrder.STATUS, 3).like(UserOrder.GOODS_NAME, goodsName)
                    .orderBy(true, UserOrder.UPDATE_TIME, false)
                    .where("not is_user_del and is_evaluate = 0"));
            } else {
                page = userOrderService.selectPage(page, new EntityWrapper<UserOrder>().eq(UserOrder.USER_ID, userId)
                    .eq(UserOrder.STATUS, 3).orderBy(true, UserOrder.UPDATE_TIME, false)
                    .where("not is_user_del and is_evaluate = 0"));
            }
        } else {
            if (goodsName != null) {
                page = userOrderService.selectPage(page, new EntityWrapper<UserOrder>().eq(UserOrder.USER_ID, userId)
                    .eq(UserOrder.STATUS, status).like(UserOrder.GOODS_NAME, goodsName)
                    .orderBy(true, UserOrder.UPDATE_TIME, false)
                    .where("not is_user_del"));
            } else {
                page = userOrderService.selectPage(page, new EntityWrapper<UserOrder>().eq(UserOrder.USER_ID, userId)
                    .eq(UserOrder.STATUS, status).orderBy(true, UserOrder.UPDATE_TIME, false)
                    .where("not is_user_del"));
            }

        }
        List<UserOrder> records = page.getRecords();
        List<UserOrderListVo> userOrderListVos = new ArrayList<>();
        for (UserOrder userOrder : records) {
            UserOrderListVo vo = new UserOrderListVo();
            vo.setUserOrder(userOrder);
            Map<String, Object> map = new HashMap<>();
            map.put(UserOrderGoods.ORDER_ID, userOrder.getId());
            List<ShopGoodsVo> shopGoodsVos = new ArrayList<>();
            List<UserOrderGoods> userOrderGoods = userOrderGoodsService.selectByMap(map);
            for (UserOrderGoods orderGoods : userOrderGoods) {
                ShopGoodsVo shopGoodsVo = new ShopGoodsVo();
                ShopGoods shopGoods = shopGoodsService.selectById(orderGoods.getGoodsId());
                if (shopGoods != null) {
                    shopGoodsVo.setGoodsId(shopGoods.getId());
                    shopGoodsVo.setGoodsName(shopGoods.getGoodsName());
                    shopGoodsVo.setMainImage(shopGoods.getMainImage());
                }
                ShopGoodsSize shopGoodsSize = shopGoodsSizeService.selectById(orderGoods.getGoodsSizeId());
                if (shopGoodsSize != null) {
                    shopGoodsVo.setSizeName(shopGoodsSize.getSizeName());
                    shopGoodsVo.setPrice(shopGoodsSize.getSalesPrice());
                }
                shopGoodsVo.setNum(orderGoods.getNum());
                shopGoodsVos.add(shopGoodsVo);
            }
            vo.setShop(shopService.selectById(userOrder.getShopId()));
            vo.setShopGoodsVos(shopGoodsVos);
            userOrderListVos.add(vo);
        }
        page.setRecords(userOrderListVos);
        return page;
    }

    @Override
    public Page getShopOrderByUserId(Page page, Wrapper wrapper, Integer shopId, Integer status, String goodsName) {
        wrapper = SqlHelper.fillWrapper(page, wrapper);
        page.setRecords(baseMapper.getShopOrderByUserId(page, wrapper, shopId, status, goodsName));
        return page;
    }

    @Override
    public Page listPage(Page page, List<String> date, String kw, OrderStatus orderStatus, Integer shopId) {
        String start = null, stop = null;
        if (date != null && !date.isEmpty()) {
            start = date.get(0);
            stop = date.get(1);
        }
        if (kw != null) {
            kw = "%" + kw.trim() + "%";
        }
        EntityWrapper wrapper = new EntityWrapper();
        if (StrUtil.isNotBlank(kw)) {
            wrapper.like(UserOrder.ORDER_NUM, kw);
        }
        if (StrUtil.isNotBlank(start) && StrUtil.isNotBlank(stop)) {
            wrapper.gt(UserOrder.CREAT_TIME, DateTime.of(start, "yyyy-MM-dd"));
            wrapper.lt(UserOrder.CREAT_TIME, DateTime.of(stop, "yyyy-MM-dd"));
        }
        if (orderStatus != null) {
            wrapper.eq(UserOrder.STATUS, orderStatus.getKey());
        }
        if (shopId != null) {
            wrapper.ne(UserOrder.IS_SHOP_DEL, 1);
            wrapper.eq(UserOrder.SHOP_ID, shopId);
        }
        wrapper.where("1=1");
        wrapper.orderBy(UserOrder.CREAT_TIME, false);
        List<UserOrderVo> list = baseMapper.findVoByPage(page, wrapper);
        return page.setRecords(list);
    }

    @Override
    public UserOrderDetailVo userOrderDetail(Integer orderId) {
        return baseMapper.userOrderDetail(orderId);
    }

    @Override
    public Integer orderCount(Integer userId) {
        EntityWrapper<UserOrder> wrapper = new EntityWrapper<>();
        wrapper.ne(UserOrder.IS_USER_DEL, 1)
            .eq(UserOrder.USER_ID, userId);
        return selectCount(wrapper);
    }

    @Override
    public Page userOrder(Page page, Integer id) {
        EntityWrapper wrapper = new EntityWrapper();
        wrapper.eq(UserOrder.USER_ID, id);
        wrapper.ne(UserOrder.IS_USER_DEL, 1);
        List<UserOrderVo> list = baseMapper.findVoByPage(page, wrapper);
        return page.setRecords(list);
    }

    @Override
    public String pushAPush(Integer orderId) {
        //获取订单详情
        UserOrderDetailVo orderDetail = userOrderDetail(orderId);
        //获取商家信息
        ShopUser shopUser = shopUserService.selectOne(new EntityWrapper<ShopUser>().eq("shop_id", orderDetail.getShopId()));
        //获取买家信息
        User user = userService.selectOne(new EntityWrapper<User>().eq("id", orderDetail.getUserId()));
        return ShopJPushUtils.pushMsg(shopUser.getId() + "",
            "订单(" + orderId + ")请尽快发货！催单人：" + user.getNickName() + "",
            "用户催单", null) ? "催单成功" : "催单失败";
    }

    /**
     * 生成订单号
     *
     * @return
     */
    public synchronized String createOrderNum() throws Exception {
        String key = RedisKeys.SHOP_GOODS_ORDER_NUM;
        Long count = null;
        if (redisUtils.hasKey(key)) {
            count = redisUtils.autoIncrement(key);
        }
        if (count == null) {
            count = (long) selectCount(null);
            redisUtils.setStr(key, String.valueOf(count));
        }
        return RandomUtil.randomNumbers(6) + String.format("%06d", count);
    }

    @Autowired
    UserOrderServiceService userOrderServiceService;

    /**
     * 生成服务单号
     *
     * @return
     */
    @Override
    public synchronized String createServiceCode() throws Exception {
        String key = RedisKeys.SHOP_GOODS_ORDER_SERVICE_CODE;
        Long count = null;
        if (redisUtils.hasKey(key)) {
            count = redisUtils.autoIncrement(key);
        }
        if (count == null) {
            count = (long) userOrderServiceService.selectCount(null);
            redisUtils.setStr(key, String.valueOf(count));
        }
        return String.format("%06d", count);
    }

    /**
     * 判断商品是不是秒杀过期
     *
     * @param goodsId
     * @param time
     * @return
     */
    public boolean isRushBuyTimeOut(Integer goodsId, Date time) {
        if (goodsId == null || time == null)
            return true;
        try {
            ShopRushBuy tm = shopGoodsSizeService.getCachedRushByTime(goodsId);
            if (tm == null) {
                return true;
            }
            Date startTime = tm.getStartTime();
            Date endTime = tm.getEndTime();
            if (startTime == null || endTime == null) {
                return true;
            }
            Date2 date2 = new Date2(time).setTime2(time.getTime());
            date2.setYear2(1970);
            date2.setMonth2(0);
            date2.setDay2(1);
            DateTime date = new DateTime(date2.getTime());
            if (date.isIn(startTime, endTime)) {
                return false;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return true;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public OrderResponse createOrder(SubmitOrderDto orderDto, ShopSubmitOrderDto orderGoods,
                                     OrderType orderType, Integer userId) throws Exception {
        boolean isTimeOut = false;
        OrderResponse response = new OrderResponse();
        response.setStatus(OrderResponseStatus.PARTLY_SUCCESS);
        response.setPayType(orderDto.getPayType());
        UserOrder o = new UserOrder();
        Date createTime = new Date();
        List<ShopSubmitOrderGoodsDto> goods = orderGoods.getGoods();
        List<UserOrderGoods> gs = new ArrayList<>();
        List<ShopSubmitOrderGoodsDto> failedGoods = new ArrayList<>();
        Iterator<ShopSubmitOrderGoodsDto> iterator = goods.iterator();
        while (iterator.hasNext()) {
            int column;
            ShopSubmitOrderGoodsDto good = iterator.next();
            if (OrderType.RUSH_BUY.equals(orderType)) {
                if (isRushBuyTimeOut(good.getGoodsId(), createTime)) {
                    isTimeOut = true;
                    continue;
                }
            }
            if (OrderType.NORMAL.equals(orderType)) {
                column = baseMapper.updateStock(good.getGoodsSizeId(), good.getNum());
            } else {
                column = baseMapper.updateKillStock(good.getGoodsSizeId(), good.getNum());
            }
            //秒杀成功
            if (column > 0) {
                UserOrderGoods g = new UserOrderGoods();
                g.setGoodsId(good.getGoodsId());
                g.setGoodsSizeId(good.getGoodsSizeId());
                g.setNum(good.getNum());
                g.setGoodsPrice(good.getGoodsPrice());
                g.setPublishTime(createTime);
                gs.add(g);
                // 秒杀不成功
            } else {
                failedGoods.add(good);
                iterator.remove();
            }
        }
        if (gs.size() > 0) {

            o.setOrderNum(createOrderNum());
            o.setDistributionTime(orderDto.getDistributionTime());
            o.setDistributionType(orderDto.getDistributionType());
            o.setCreatTime(createTime);
            o.setStatus(OrderStatus.DUE_PAY.getKey());
            o.setIsUserDel(0);
            o.setIsShopDel(0);
            o.setIsClosed(0);
            o.setShopId(orderGoods.getShopId());
            o.setAddressId(orderDto.getAddressId());
            o.setUserId(userId);
            o.setCouponId(orderGoods.getUserCouponId());
            o.setOrderType(orderDto.getOrderType());
//            o.setIntegralRuleId(orderGoods.getIntegralRuleId());
//            o.setFullReduceId(orderGoods.getFullReduceId());
            OrderPrice orderPrice = orderPrice(orderGoods, orderType, userId, orderDto.getIsUseIntegral());
            o.setOrderPrice(orderPrice.getOrderPrice());
            o.setOrderRealPrice(orderPrice.getOrderRealPrice());
            o.setCouponReduce(orderPrice.getCouponReduce());
            o.setIntegralReduce(orderPrice.getIntegralReduce());
            o.setPayType(orderDto.getPayType());
            o.setFreight(orderPrice.getFreight());
            o.setUpdateTime(new Date());
            StringBuilder goodsName = new StringBuilder();
            //创建一个邮费计数值；
                Integer you=0;
            //创建一个总价计数值；
            BigDecimal zong =new BigDecimal(0);

            for (UserOrderGoods userOrderGoods : gs) {
                //获取商品型号数量
                Integer num = userOrderGoods.getNum();
                BigDecimal nums = new BigDecimal(num);
                //获取购买价格
                BigDecimal goodsPrice = userOrderGoods.getGoodsPrice();
                 zong =  zong.add(nums.multiply(goodsPrice));
                //获取商品型号id
                Integer goodsSizeId = userOrderGoods.getGoodsSizeId();
                //根据型号查询邮费；
                ShopGoodsSize sgs = new ShopGoodsSize();
                ShopGoodsSize ss = sgs.selectById(goodsSizeId);
                //取出型号id的邮费；
                if(ss!=null){
                    int i = Integer.parseInt(ss.getFreight());
                    you=you+i;
                }

                ShopGoods shopGoods = shopGoodsService.selectById(userOrderGoods.getGoodsId());
                goodsName.append(shopGoods.getGoodsName());
                goodsName.append(",");
            }
            //满减数量
            BigDecimal discount=new BigDecimal(0);
            //查询是否满减
            List<Coupon> lb = couponService.getListByShopId(orderGoods.getShopId());
            for(Coupon cu : lb){
                //获取满减值
                BigDecimal fullPrice = cu.getFullPrice();
                if(zong.compareTo(fullPrice)>=0){
                    discount = cu.getDiscount();
                    break;
                }
            }
            //查询是否包邮；
            ShopSets shopSet = shopSetService.getShopSet(orderGoods.getShopId());
            if(shopSet==null){

            }else{
                //获取包邮数据
                Double money = shopSet.getMoney();
                BigDecimal baoy = new BigDecimal(money);
                //如果价格大于包邮量邮费为0
                if(zong.compareTo(baoy)>=0){

                    o.setOrderPrice(zong);
                    o.setOrderRealPrice(zong.subtract(discount));
                }else{
                    //不然将邮费和商品价和满减相加减起来
                    BigDecimal yf = new BigDecimal(you);
                    o.setFreight(yf);
                    o.setOrderPrice(zong);
                    o.setOrderRealPrice(zong.add(yf).subtract(discount));
                }
            }




            //设置满减；
            o.setFullReduce(discount);
            o.setGoodsName(goodsName.toString());
            o.insert();
            Integer orderId = o.getId();
            response.setOrderId(orderId);
            response.setOrderNum(o.getOrderNum());
            response.setOrderPrice(orderPrice);
            for (UserOrderGoods g : gs) {
                g.setOrderId(orderId);
                g.insert();
            }
        }
        if (goods.size() == gs.size() && gs.size() > 0) {
            response.setStatus(OrderResponseStatus.SUCCESS);
        } else if (gs.size() == 0) {
            response.setStatus(OrderResponseStatus.UNDER_STOCK);
        } else {
            response.setStatus(OrderResponseStatus.PARTLY_SUCCESS);
        }
        if (isTimeOut) {
            response.setStatus(OrderResponseStatus.TIME_OUT);
        }
        return response;
    }

    /**
     * 清除购物车
     *
     * @param dto
     */
    public void delShopCart(ShopSubmitOrderDto dto) {
        for (ShopSubmitOrderGoodsDto goods : dto.getGoods()) {
            Integer cartId = goods.getCartId();
            if (cartId == null)
                continue;
            shoppingCartService.deleteById(cartId);
        }
    }

    @Override
    public List<OrderResponse> submit(SubmitOrderDto orderDto, Integer userId) throws Exception {
        List<OrderResponse> list = new ArrayList<>();
        List<ShopSubmitOrderDto> shops = orderDto.getShops();
        OrderType orderType = EnumUitl.toEnum(OrderType.class, orderDto.getOrderType());
        for (ShopSubmitOrderDto shop : shops) {
            if (shop.getGoods().size() == 0) {
                continue;
            }
            OrderResponse response = createOrder(orderDto, shop, orderType, userId);
            if (OrderType.NORMAL.equals(orderType) &&
                (response.is(OrderResponseStatus.SUCCESS) || response.is(OrderResponseStatus.PARTLY_SUCCESS))) {
                delShopCart(shop);
            }
            list.add(response);
        }
        return list;
    }

    /**
     * 用户-购物车-去结算
     * * 金额计算（折扣、优惠券、积分来计算订单价格）
     * * 计算订单金额
     * * 更新用户积分总数
     */
    @Override
    public OrderPrice orderPrice(ShopSubmitOrderDto orderDto, OrderType orderType, Integer userId, Integer isUseIntegral) throws MessageException {
        OrderPrice orderPrice = new OrderPrice();
        //先计算没有使用任何优惠的订单原价
        BigDecimal totalPriceWithOutDiscount = new BigDecimal("0.00");
        //获取商品列表
        ArrayList<ShopSubmitOrderGoodsDto> goodsList = orderDto.getGoods();
        for (ShopSubmitOrderGoodsDto goodsDto : goodsList) {
            Integer goodsSizeId = goodsDto.getGoodsSizeId();   //商品规格id
            ShopGoodsSize shopGoodsSize = shopGoodsSizeService.selectOne(new EntityWrapper<ShopGoodsSize>()
                .eq(ShopGoodsSize.ID, goodsSizeId)
            );
            //订单项的价格
            if (shopGoodsSize != null) {
                if (shopGoodsSize.getStock() < goodsDto.getNum()) {
                    throw new MessageException(shopGoodsService.selectById(goodsDto.getGoodsId()).getGoodsName() + "库存不足!");
                }
                BigDecimal orderItemPrice = goodsDto.getGoodsPrice().multiply(new BigDecimal(goodsDto.getNum()));
                totalPriceWithOutDiscount = totalPriceWithOutDiscount.add(orderItemPrice);
            }
        }
        //设置原始价格
        orderPrice.setOrderPrice(totalPriceWithOutDiscount.setScale(2));

        //店铺id
        Integer shopId = orderDto.getShopId();
        //优惠券id
        Integer userCouponId = orderDto.getUserCouponId();
        //根据优惠券id和shopId查询对应的优惠券
        Coupon coupon = couponService.selectOne(new EntityWrapper<Coupon>()
            .eq(Coupon.ID, userCouponId)
            .eq(Coupon.SHOP_ID, shopId)
            .eq(Coupon.IS_DEL, 0)
        );
        if (coupon != null) {
            BigDecimal discount = new BigDecimal("0.00");     //优惠券折扣
            //优惠券开始时间和结束时间判断
            if (new Date().after(coupon.getStartTime())
                && new Date().before(coupon.getEndTime())
                && totalPriceWithOutDiscount.doubleValue() >= coupon.getFullPrice().doubleValue()) {
                //可以使用优惠券
                //获取优惠券折扣
                discount = coupon.getDiscount();
                //设置用戶优惠卷状态(已使用)
                UserCoupon userCoupon = new UserCoupon();
                userCoupon.setId(userCouponId);
                userCoupon.selectById();
                userCoupon.setIsUsed(1);
                userCoupon.updateById();
            }
            totalPriceWithOutDiscount = totalPriceWithOutDiscount.subtract(discount);   //减去优惠券的折扣
            orderPrice.setCouponReduce(discount.setScale(2));         //优惠券优惠了多少
        }
        if (isUseIntegral == 1) {
            //计算积分抵扣
            //查询用户总积分
            Integer integralNum = userIntegralService.getIntegral(userId);    //总积分
            //积分抵扣与规则
            IntegralRule integralRule = integralRuleService.selectOne(new EntityWrapper<IntegralRule>()
                .eq(IntegralRule.ID, 1)
            );
            //使用的积分
            Integer useIntegral;
            //积分可以抵扣多少钱
            BigDecimal integralReduce = new BigDecimal(integralNum / integralRule.getIntegral()).multiply(integralRule.getDeduction());
            if (integralReduce.compareTo(totalPriceWithOutDiscount) > 0) {
                useIntegral = totalPriceWithOutDiscount.intValue() / integralRule.getDeduction().intValue() * integralRule.getIntegral();
            } else {
                useIntegral = integralNum - integralNum % integralRule.getIntegral();
            }
            //积分抵扣价格
            orderPrice.setIntegralReduce(integralReduce.setScale(2));
            //减去积分抵扣价格
            totalPriceWithOutDiscount = totalPriceWithOutDiscount.subtract(integralReduce);
            //积分交易记录
            UserIntegral userIntegral = new UserIntegral();
            userIntegral.setIntegralNumber(useIntegral);
            userIntegral.setUserId(userId);
            userIntegral.setIntegralType(-1);
            userIntegral.insert();
        }


        //设置运费
        BigDecimal freight = new BigDecimal("0.00");
        for (ShopSubmitOrderGoodsDto goodsDto : goodsList) {
            Integer goodsSizeId = goodsDto.getGoodsSizeId();   //商品规格id
            ShopGoodsSize shopGoodsSize = shopGoodsSizeService.selectOne(new EntityWrapper<ShopGoodsSize>()
                .eq(ShopGoodsSize.ID, goodsSizeId)
            );
            //订单项的价格
            if (shopGoodsSize != null && shopGoodsSize.getFullFreight() != null) {
                //不符合包邮
                if (new BigDecimal(shopGoodsSize.getFullFreight()).compareTo(orderPrice.getOrderPrice()) < 0) {
                    freight = freight.add(new BigDecimal(shopGoodsSize.getFreight()));
                }
            }
        }
        //添加运费
        totalPriceWithOutDiscount = totalPriceWithOutDiscount.add(freight);
        orderPrice.setFreight(freight.setScale(2));

        orderPrice.setOrderRealPrice(totalPriceWithOutDiscount.setScale(2));//订单实际价格
        return orderPrice;
    }

    @Override
    public void balancePay(List<UserOrder> userOrders, Integer userId) throws Exception {
        //获取订单价格
        BigDecimal price = new BigDecimal("0.00");
        for (UserOrder u : userOrders) {
            price = price.add(u.getOrderRealPrice());
        }
        //用户余额不足
        UserRemainderInfo remainder = userRemainderService.getRemainder(userId);
        if (!remainder.hasRemain(price)) {
            throw new MessageException("余额不足");
        }
        for (UserOrder userOrder : userOrders) {
            //用户交易记录
            UserRemainder userRemainder = new UserRemainder();
            userRemainder.setCreateTime(new Date());
            userRemainder.setRemainder(userOrder.getOrderRealPrice());
            userRemainder.setType(UserRemainderType.CONSUME);
            userRemainder.setUserId(userOrder.getUserId());
            userRemainder.setOrderNum(userOrder.getOrderNum());
            userRemainder.setStatus(UserRemainderStatus.PASSED);
            userRemainder.setPlatformNumber(userOrder.getPlatformNumber());
            userRemainder.insert();
            userRemainderService.consume(userId,userOrder.getOrderRealPrice());
            //修改订单信息
            userOrder.setStatus(OrderStatus.WAIT_DELIVER.getKey());
            userOrder.setPayType(PayType.BALANCE_PAY.getKey());
            userOrder.setPayTime(new Date());
            userOrder.updateById();
        }
        //商家余额
        ShopMoney shopMoney = new ShopMoney();
        shopMoney.setMoney(price.toString());
        shopMoney.setPublishTime(new Date());
        shopMoney.setType(1);
        shopMoney.setShopId(userOrders.get(0).getShopId());
        shopMoney.insert();
    }


    @Override
    public AfterSaleVo getAddressAndPhone(Integer orderId) {
        return baseMapper.getAddressAndPhone(orderId);
    }

    @Override
    public PayResult payComplete(List<UserOrder> userOrders) {
        PayResult payResult = new PayResult();
        payResult.setOrderNum(userOrders.get(0).getOrderNum());
        payResult.setPayType(userOrders.get(0).getPayType());
        payResult.setPayName(userService.selectById(userOrders.get(0).getUserId()).getPhone());
        BigDecimal price = new BigDecimal("0.00");
        for (UserOrder u : userOrders) {
            price = price.add(u.getOrderRealPrice());
        }
        payResult.setPrice(price);
        payResult.setReceiver(userAddressService.selectById(userOrders.get(0).getAddressId()).getName());
        return payResult;
    }

    @Override
    public void refund(Integer id, String price) throws MessageException {
        UserOrder order = userOrderService.selectById(id);
        //商家余额
        if (shopMoneyService.getShopMoney(order.getShopId()).compareTo(new BigDecimal(price)) < 0) {
            throw new MessageException("余额不足");
        }
        //用户交易记录
        UserRemainder userRemainder = new UserRemainder();
        userRemainder.setCreateTime(new Date());
        userRemainder.setRemainder(new BigDecimal(price));
        userRemainder.setType(UserRemainderType.CONSUME);
        userRemainder.setUserId(order.getUserId());
        userRemainder.setOrderNum(order.getOrderNum());
        userRemainder.setStatus(UserRemainderStatus.PASSED);
        userRemainder.setPlatformNumber(order.getPlatformNumber());
        userRemainder.insert();


        //商家余额
        ShopMoney shopMoney = new ShopMoney();
        shopMoney.setMoney(price);
        shopMoney.setPublishTime(new Date());
        shopMoney.setType(0);
        shopMoney.setShopId(order.getShopId());
        shopMoney.insert();
    }


}
