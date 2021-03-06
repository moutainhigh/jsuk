package com.jh.jsuk.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.egzosn.pay.ali.api.AliPayConfigStorage;
import com.egzosn.pay.ali.api.AliPayService;
import com.egzosn.pay.ali.bean.AliTransactionType;
import com.egzosn.pay.common.api.PayService;
import com.egzosn.pay.common.bean.PayOrder;
import com.egzosn.pay.common.http.UriVariables;
import com.egzosn.pay.common.util.XML;
import com.egzosn.pay.wx.api.WxPayConfigStorage;
import com.egzosn.pay.wx.api.WxPayService;
import com.egzosn.pay.wx.bean.WxTransactionType;
import com.github.wxpay.sdk.WXPayUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jh.jsuk.conf.Session;
import com.jh.jsuk.entity.*;
import com.jh.jsuk.entity.vo.GoodsVo;
import com.jh.jsuk.entity.vo.ShoppingCartVo;
import com.jh.jsuk.envm.UserRemainderStatus;
import com.jh.jsuk.envm.UserRemainderType;
import com.jh.jsuk.service.*;
import com.jh.jsuk.service.UserOrderService;
import com.jh.jsuk.utils.*;
import com.jh.jsuk.utils.wx.JsapiTicketUtil;
import com.jh.jsuk.utils.wx.SHA1;
import com.jh.jsuk.utils.wx.WxPay;
import com.pingplusplus.exception.ChannelException;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * luopa 在 2018/6/21 创建.
 */
@Api(tags = {"用户支付相关操作API:"})
@RestController
@RequestMapping("/pay")
@PropertySource(value = "classpath:payinfo.properties")
public class PayController {
    private static final Logger logger = LoggerFactory.getLogger(PayController.class);
    @Autowired
    UserOrderService userOrderService;
    //    @Autowired
//    MemberConfigService memberConfigService;
    @Autowired
    UserRemainderService userRemainderService;
    @Autowired
    UserService userService;
    @Autowired
    ShoppingCartService shoppingCartService;
    @Autowired
    private ExpressService expressService;
    @Autowired
    ShopGoodsService shopGoodsService;
    @Autowired
    UserOrderGoodsService userOrderGoodsService;
    @Autowired
    ShopMoneyService shopMoneyService;
    //支付宝服务
    private PayService aliservice = null;
    //微信服务
    private PayService wxservice = null;
    @Resource
    private Environment env;
    @Autowired
    private ShopService shopService;

    /**
     * 加载支付配置信息
     */
    @PostConstruct
    public void init() {
        /*读取微信配置信息*/
        String wx_appid = env.getProperty("APP_ID");
        String wx_mchid = env.getProperty("MCH_ID");
        String wx_api_key = env.getProperty("API_KEY");
        String wx_sign_type = env.getProperty("SIGN_TYPE");
        String wx_notifyurl = env.getProperty("NOTIFYURL");
        String wx_inputcharset = env.getProperty("INPUTCHARSET");
        String wx_paytype = env.getProperty("WX_PAYTYPE");
        /*读取支付宝配置信息*/
        String ali_pid = env.getProperty("pid");
        String ali_appid = env.getProperty("appid");
        String ali_public_key = env.getProperty("public_key");
        String ali_alipay_public_key = env.getProperty("alipay_public_key");
        String ali_notifyUrl = env.getProperty("notifyUrl");
        String ali_signType = env.getProperty("signType");
        String ali_seller = env.getProperty("seller");
        String ali_inputCharset = env.getProperty("inputCharset");
        Boolean ali_test = Boolean.valueOf(env.getProperty("test"));
        String ali_ZFB_PAYTYPE = env.getProperty("ZFB_PAYTYPE");

        //初始化支付宝
        AliPayConfigStorage aliPayConfigStorage = new AliPayConfigStorage();
        aliPayConfigStorage.setPid(ali_pid);
        aliPayConfigStorage.setAppId(ali_appid);
        //支付宝公钥
        aliPayConfigStorage.setKeyPublic(ali_public_key);
        //私钥
        aliPayConfigStorage.setKeyPrivate(ali_alipay_public_key);
        //收款号
        aliPayConfigStorage.setSeller(ali_seller);
        aliPayConfigStorage.setNotifyUrl(ali_notifyUrl);
        aliPayConfigStorage.setSignType(ali_signType);
        aliPayConfigStorage.setInputCharset(ali_inputCharset);
        //是否为测试账号，沙箱环境
        aliPayConfigStorage.setTest(ali_test);
        aliPayConfigStorage.setPayType(ali_ZFB_PAYTYPE);
        aliservice = new AliPayService(aliPayConfigStorage);

        //初始化微信
        WxPayConfigStorage wxPayConfigStorage = new WxPayConfigStorage();
        wxPayConfigStorage.setMchId(wx_mchid);
        wxPayConfigStorage.setAppid(wx_appid);
        wxPayConfigStorage.setKeyPrivate(wx_api_key);
        wxPayConfigStorage.setNotifyUrl(wx_notifyurl);
        wxPayConfigStorage.setSignType(wx_sign_type);
        wxPayConfigStorage.setInputCharset(wx_inputcharset);
        wxPayConfigStorage.setPayType(wx_paytype);
        wxservice = new WxPayService(wxPayConfigStorage);
    }

    /**
     * 生成系统订单信息
     *
     * @param userId
     * @param goodsId
     * @param carId
     * @return
     */
    @ApiOperation("下单 在本地系统生成订单")
    @ApiImplicitParams(value = {
        @ApiImplicitParam(name = "userId", value = "用户ID",
            required = true, paramType = "query", dataType = "string"),
        @ApiImplicitParam(name = "goodsId", value = "商品ID",
            required = true, paramType = "query", dataType = "string"),
        @ApiImplicitParam(name = "carId", value = "购物车ID",
            required = false, paramType = "query", dataType = "string"),
        @ApiImplicitParam(name = "orderType", value = "订单类型 0:普通订单 1:秒杀订单 2:会员购买 3:充值 4:到店支付",
            required = true, paramType = "query", dataType = "string"),
        @ApiImplicitParam(name = "memberConfigId", value = "充值配置ID",
            required = false, paramType = "query", dataType = "string"),
        @ApiImplicitParam(name = "paymentAmount", value = "到店支付金额",
            required = true, paramType = "query", dataType = "String")
    })
    @Transactional
    @RequestMapping(value = "placeAnOrder", method = RequestMethod.POST)
    public Result placeAnOrder(Integer userId, String goodsId, String carId, String orderType, Integer memberConfigId, String shopUserId, String paymentAmount) {
        if (ObjectUtil.isNull(userId)) {
            return new Result().erro("参数错误!");
        }
        Map<String, Object> resultMap = Maps.newHashMap();
        //订单类型 0:普通订单 1:秒杀订单 2:会员购买 3:充值 4:到店支付
        //会员购买
        if (StrUtil.equals(orderType, "2")) {
            //查询充值配置
//            MemberConfig memberConfig = memberConfigService.selectOne(new MyEntityWrapper<MemberConfig>().eq(MemberConfig.ID, memberConfigId));
            UserRemainder userRemainder = new UserRemainder();
            userRemainder.setUserId(userId);
            userRemainder.setOrderNum("JSUKVIP" + RandomUtil.randomNumbers(18));
//            userRemainder.setRemainder(new BigDecimal(memberConfig.getMemberPrice()));
            userRemainder.setMemberId(memberConfigId);
            userRemainder.setStatus(UserRemainderStatus.PASSED);
            userRemainder.setType(UserRemainderType.CONSUME);
            userRemainder.setCreateTime(new Date());
            //新增购买充值记录
            userRemainder.insert();

            resultMap.put("orderType", orderType);
            resultMap.put("orderId", userRemainder.getOrderNum());
            return new Result().success(resultMap);
        } else if (StrUtil.equals(orderType, "0") || StrUtil.equals(orderType, "1")) {
            //购物车订单
            if (StrUtil.isNotBlank(carId)) {
                List<ShoppingCartVo> shoppingCartVos = shoppingCartService.selectVoList(String.valueOf(userId), "");
                logger.info("购物车商品数量:{}", shoppingCartVos.size());
                Map<String, GoodsVo> goodMaps = Maps.newHashMap();
                for (ShoppingCartVo shoppingCartVo : shoppingCartVos) {
                    List<GoodsVo> goods = shoppingCartVo.getGoods();
                    for (GoodsVo good : goods) {
                        //查询商品是否存在
                        int count = shopGoodsService.selectCount(new MyEntityWrapper<ShopGoods>().eq(ShopGoods.ID, good.getGoodsId()));
                        if (good.getChecked() == 1 && good.getNum() > 0 && count > 0) {
                            goodMaps.put(good.getGoodsId(), good);
                        }
                    }
                }

                UserOrder userOrder = new UserOrder();
                userOrder.setOrderNum("JSUKOF" + RandomUtil.randomNumbers(18));
                userOrder.setStatus(0);
                userOrder.setUserId(userId);
                userOrder.setRemark(shopUserId);
                userOrder.setOrderType(Integer.valueOf(orderType));
                userOrder.setCreatTime(new Date());
                userOrder.setOrderPrice(new BigDecimal(paymentAmount));
                //新增到店支付订单记录
                userOrder.insert();

                List<UserOrderGoods> list = Lists.newArrayList();
                //订单商品信息
                goodMaps.forEach((k, v) -> {
                    UserOrderGoods userOrderGoods = new UserOrderGoods();
                    userOrderGoods.setGoodsId(Integer.valueOf(k));
                    if (StrUtil.equals(orderType, "1")) {
                        userOrderGoods.setGoodsPrice(new BigDecimal(v.getKillPrice()));
                    } else if (StrUtil.equals(orderType, "0")) {
                        userOrderGoods.setGoodsPrice(new BigDecimal(v.getSalesPrice()));
                    }
                    userOrderGoods.setPublishTime(new Date());
                    userOrderGoods.setGoodsId(Integer.valueOf(k));
                    userOrderGoods.setOrderId(userOrder.getId());
                    userOrderGoods.setNum(v.getNum());
                    list.add(userOrderGoods);
                });
                logger.error("订单商品信息数量:{}", list.size());
                boolean batch = userOrderGoodsService.insertBatch(list);
                logger.error("批量添加商品订单信息: {}", batch ? "成功" : "失败");
                resultMap.put("orderType", orderType);
                resultMap.put("orderId", userOrder.getOrderNum());
                return new Result().success(resultMap);
            }
            //直接商品订单
            if (StrUtil.isNotBlank(goodsId)) {
                UserOrder userOrder = new UserOrder();
                userOrder.setOrderNum("JSUKOF" + RandomUtil.randomNumbers(18));
                userOrder.setStatus(0);
                userOrder.setUserId(userId);
                userOrder.setRemark(shopUserId);
                userOrder.setOrderType(Integer.valueOf(orderType));
                userOrder.setCreatTime(new Date());
                userOrder.setOrderPrice(new BigDecimal(paymentAmount));
                //新增到店支付订单记录
                userOrder.insert();

                resultMap.put("orderType", orderType);
                resultMap.put("orderId", userOrder.getOrderNum());
                return new Result().success(resultMap);
            }
            return new Result().erro("参数错误");
        } else if (StrUtil.equals(orderType, "3")) {
            UserRemainder userRemainder = new UserRemainder();
            userRemainder.setUserId(userId);
            userRemainder.setOrderNum("JSUKCZ" + RandomUtil.randomNumbers(18));
            userRemainder.setRemainder(new BigDecimal(paymentAmount));
            userRemainder.setStatus(UserRemainderStatus.PASSED);
            userRemainder.setType(UserRemainderType.CONSUME);
            userRemainder.setCreateTime(new Date());
            //新增用户充值记录
            userRemainder.insert();

            resultMap.put("orderType", orderType);
            resultMap.put("orderId", userRemainder.getOrderNum());
            return new Result().success(resultMap);
        } else if (StrUtil.equals(orderType, "4")) {
            UserOrder userOrder = new UserOrder();
            userOrder.setOrderNum("JSUKOF" + RandomUtil.randomNumbers(18));
            userOrder.setStatus(0);
            userOrder.setUserId(userId);
            userOrder.setRemark(shopUserId);
            userOrder.setOrderType(Integer.valueOf(orderType));
            userOrder.setCreatTime(new Date());
            userOrder.setOrderPrice(new BigDecimal(paymentAmount));
            //新增到店支付订单记录
            userOrder.insert();

            resultMap.put("orderType", orderType);
            resultMap.put("orderId", userOrder.getOrderNum());
            return new Result().success(resultMap);
        } else {
            return new Result().erro("参数错误!");
        }
    }

    /**
     * 预下单
     *
     * @param orderId     商户订单ID
     * @param payWay      支付方式
     * @param userId      用户Id
     * @param isDeduction 是否抵扣  1:是  0:否
     * @return
     */
    @ApiOperation("预下单 在支付系统生成订单")
    @ApiImplicitParams(value = {
        @ApiImplicitParam(name = "orderId", value = "订单号",
            required = true, paramType = "query", dataType = "string"),
        @ApiImplicitParam(name = "payWay", value = "支付方式 0:支付宝 1:微信",
            required = true, paramType = "query", dataType = "string"),
        @ApiImplicitParam(name = "userId", value = "用户ID",
            required = true, paramType = "query", dataType = "string"),
        @ApiImplicitParam(name = "orderType", value = "订单类型 0:普通订单 1:秒杀订单 2:会员充值",
            required = true, paramType = "query", dataType = "string"),
        @ApiImplicitParam(name = "isDeduction", value = "是否使用折扣 1:是 0:否",
            required = true, paramType = "query", dataType = "string")
    })
    @RequestMapping(value = "pre_order", method = RequestMethod.POST)
    @Transactional(propagation = Propagation.REQUIRED)
    public ServerResponse appPay(String orderId, String payWay, Integer userId, String orderType, String isDeduction) {
        Callable<ServerResponse> callable = () -> {
            if (StrUtil.isBlank(orderType) || StrUtil.isBlank(isDeduction) || StrUtil.isBlank(payWay)) {
                return ServerResponse.createByErrorMessage("参数错误!");
            }
            UserRemainder userRemainder = null;
            PayOrder payOrder = null;
            //当为普通/秒杀订单时OrderId必须
            if (StrUtil.equals(orderType, "1") || StrUtil.equals(orderType, "0")) {
                /*if (StrUtil.isBlank(orderId)) {
                    return ServerResponse.createByErrorMessage("参数错误!");
                }
                orderRecord = userOrderService.selectOne(new MyEntityWrapper<UserOrder>().eq(UserOrder.ORDER_NUM, orderId).eq(UserOrder.USER_ID, userId)
                        .eq(UserOrder.STATUS, "0")
                );
                if (orderRecord == null) {
                    return ServerResponse.createByErrorMessage("订单不存在");
                }
                if (orderRecord.getStatus() > 2) {
                    return ServerResponse.createByErrorMessage("订单已支付,请勿重复支付");
                }*/
                String[] orderIds = orderId.split(",");
                List<UserOrder> userOrders = userOrderService.selectBatchIds(new ArrayList<>(Arrays.asList(orderIds)));
                BigDecimal price = new BigDecimal("0.00");
                for (UserOrder userOrder : userOrders) {
                    price = price.add(userOrder.getOrderRealPrice());
                }
                payOrder = new PayOrder("巨商U客订单" + orderId, "巨商U客订单", price, orderId);
            }

            //当为购买订单时memberConfigId必须
            if (StrUtil.equals(orderType, "2")) {
                userRemainder = userRemainderService.selectOne(new MyEntityWrapper<UserRemainder>().eq(UserRemainder.ORDER_NUM, orderId)
                    .eq(UserRemainder.STATUS, "0")
                );
                if (userRemainder == null) {
                    return ServerResponse.createByErrorMessage("订单不存在");
                }
                if (UserRemainderStatus.PASSED.equals(userRemainder.getRemainder())) {
                    return ServerResponse.createByErrorMessage("订单已支付,请勿重复支付");
                }
                payOrder = new PayOrder("巨商U客订单" + userRemainder.getOrderNum(), "巨商U客充值会员订单", userRemainder.getRemainder(), orderId);
            }

            //支付方式(0:支付宝，1:微信，2:苹果内购)
            if (StrUtil.equals(payWay, "0")) {
                //TODO 支付宝支付
                payOrder.setTransactionType(AliTransactionType.APP);
                Map<String, Object> orderInfo = aliservice.orderInfo(payOrder);
                System.out.println("\n" + orderInfo);
                //Map转化为对应得参数字符串
                return ServerResponse.createBySuccess(UriVariables.getMapToParameters(aliservice.orderInfo(payOrder)));
            }
            if (StrUtil.equals(payWay, "1")) {
                //TODO 微信支付
                payOrder.setTransactionType(WxTransactionType.APP);
                Map<String, Object> orderInfo = wxservice.orderInfo(payOrder);
                //Map转化为对应得参数字符串
                return ServerResponse.createBySuccess(orderInfo);
            }
            if (StrUtil.equals(payWay, "2")) {
                //TODO 苹果内购
                //扣除余额
            }
            return ServerResponse.createByErrorMessage("参数错误!");
        };
        try {
            return callable.call();
        } catch (Exception e) {
            e.printStackTrace();
            return ServerResponse.createByErrorMessage("系统繁忙,请稍后再试");
        }
    }

    /**
     * 支付宝回调
     *
     * @param request
     * @return
     */
    @ApiIgnore
    @RequestMapping(value = "alipayBack.json", method = {RequestMethod.POST, RequestMethod.GET})
    public String alipayBack(HttpServletRequest request) throws IOException {
        //获取支付方返回的对应参数
        Map<String, Object> params = aliservice.getParameter2Map(request.getParameterMap(), request.getInputStream());
        params.forEach((k, v) -> System.out.println(k + ":" + v));
        //校验
        if (aliservice.verify(params)) {
            //这里处理业务逻辑
            boolean flag = businessProcess(params, 0);
            if (flag) {
                return aliservice.getPayOutMessage("success", "成功").toMessage();
            } else {
                return aliservice.getPayOutMessage("fail", "失败").toMessage();
            }
        }
        logger.info(UriVariables.getMapToParameters(params));
        logger.error("验签失败");
        return aliservice.getPayOutMessage("fail", "失败").toMessage();
    }

    private boolean businessProcess(Map<String, Object> params, Integer payWay) {
        boolean flag = false;
        //商户订单号
        String outTradeNo = params.get("out_trade_no") == null ? null : params.get("out_trade_no").toString();
        //这里处理业务逻辑
        logger.info("订单号 " + outTradeNo);
        String[] orderIds = outTradeNo.split(",");
        List<UserOrder> userOrders = userOrderService.selectBatchIds(new ArrayList<>(Arrays.asList(orderIds)));
//        UserOrder userOrder = userOrderService.selectOne(new MyEntityWrapper<UserOrder>().eq(UserOrder.ORDER_NUM, outTradeNo).eq(UserOrder.STATUS, "0"));
//        UserRemainder userRemainder = userRemainderService.selectOne(new MyEntityWrapper<UserRemainder>().eq(UserRemainder.ORDER_NUM, outTradeNo).eq(UserRemainder.IS_OK, "0"));
        if (userOrders.size() == 0) {
            logger.error("订单不存在");
            return flag;
        } else {
            //平台流水
            String platform_number = null;
            if (params != null) {
                if (payWay == 0) {
                    platform_number = String.valueOf(params.get("trade_no"));
                } else if (payWay == 1) {
                    platform_number = String.valueOf(params.get("transaction_id"));
                }
            }
            for (UserOrder userOrder : userOrders) {
                UserRemainder userRemainder = userRemainderService.selectOne(new MyEntityWrapper<UserRemainder>().eq(UserRemainder.ORDER_NUM, userOrder.getOrderNum()).eq(UserRemainder.STATUS, UserRemainderStatus.APPLYING));
                if (userOrder != null) {
                    //TODO 普通订单
                    //0待付款  1待发货  2=已发货 3=交易成功 4=申请退款 5=退款成功 6=交易关闭 7=售后
                    if (userOrder.getStatus() > 0) {
                        flag = true;
                    }
                    try {
                        userOrder.setStatus(1);
                        userOrder.setPayTime(new Date());
                        userOrder.setPlatformNumber(platform_number);
                        if (userOrder.updateById()) {
                            flag = true;
                        } else {
                            return false;
                        }
                    } catch (Exception e) {
                        logger.error("更改订单状态失败----{}", e.getMessage());
                        return false;
                    }
                } else if (userRemainder != null) {
                    //0待付款  1已付款
                    if (UserRemainderStatus.PASSED.equals(userRemainder.getStatus())) {
                        return true;
                    } else {
                        //类型 1=充值,-1=消费,0=其他,2=购买会员
                        Integer type = userRemainder.getType().getKey();
                        logger.info("充值类型--->{}", type);
                        //TODO 充值订单
                        try {
                            switch (type) {
                                case 2:
//                                    MemberConfig memberConfig = memberConfigService.selectOne(new MyEntityWrapper<MemberConfig>().eq(MemberConfig.ID, userRemainder.getMemberId()));
                                    //更新会员状态
                                    User user = userService.selectById(userRemainder.getUserId());
//                                    user.setLevel(memberConfig.getId());
                                    user.updateById();
                                case 1:
                                    userRemainder.setStatus(UserRemainderStatus.PASSED);
                                    userRemainder.setPlatformNumber(platform_number);
                                    //更新充值订单状态
                                    userRemainder.updateById();
                                    break;
                            }
                            return true;
                        } catch (Exception e) {
                            logger.error("更改充值订单失败----{}", e.getMessage());
                            return false;
                        }
                    }
                }
            }
        }
        return flag;
    }

    /**
     * 微信回调
     *
     * @param request
     * @return
     * @throws IOException
     */
    @ApiIgnore
    @RequestMapping(value = "wxpayBack.json", method = {RequestMethod.POST, RequestMethod.GET})
    public String wxpayBack(HttpServletRequest request) throws IOException {
        //获取支付方返回的对应参数
        Map<String, Object> params = wxservice.getParameter2Map(request.getParameterMap(), request.getInputStream());
        params.forEach((k, v) -> System.out.println(k + ":" + v));
        String requestXML = XML.getMap2Xml(params);
        System.out.println(requestXML);
        //校验
        if (wxservice.verify(params)) {
            //验签成功
            boolean flag = businessProcess(params, 1);
            if (flag) {
                return wxservice.getPayOutMessage("success", "成功").toMessage();
            } else {
                return wxservice.getPayOutMessage("fail", "失败").toMessage();
            }
        }
        logger.info(UriVariables.getMapToParameters(params));
        logger.error("验签失败");
        return wxservice.getPayOutMessage("fail", "失败").toMessage();
    }


    /**
     * 交易查询
     *
     * @param orderId 商户订单ID
     * @param userId  用户ID
     * @return
     */
   /* @RequestMapping("qRCodePay")
    @ResponseBody
    public ServerResponse<Map<String, Object>> qRCodePay(String orderId, String userId) {
        Callable<ServerResponse<Map<String, Object>>> callable = () -> {

            UserOrderRecord orderRecord = userOrderRecordService.orderParticulars(orderId);
            PayOrder payOrder = new PayOrder("订单" + orderRecord.getModuleName(), orderRecord.getModuleName(), orderRecord.getPayMoney(), orderId);
            payOrder.setTransactionType(AliTransactionType.APP);
            Map<String, Object> orderInfo = aliservice.orderInfo(payOrder);
            System.out.println(orderInfo);
            Map<String, Object> data = new HashMap<>();
            data.put("orderInfo", UriVariables.getMapToParameters(aliservice.orderInfo(payOrder)));

            //获取表单提交对应的字符串，将其序列化到页面即可,
            String directHtml = aliservice.buildRequest(orderInfo, MethodType.POST);
            if (null != directHtml) {
                System.out.println(directHtml);
            }
            return null;
        };
        try {
            return callable.call();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }*/


    /**
     * 乡村旅游定金支付
     */
    // @RequestMapping(value="/payDingJin",method=RequestMethod.POST)
    public Result payDingJin(String totalAmount, String userId, Integer activity_id) {
        if (Double.parseDouble(totalAmount) <= 0) {
            return new Result().erro("支付金额错误");
        }
        /*
        封装参数,创建微信交易对象
         */
        Map<String, Object> paramMap = new HashMap<>();

        paramMap.put("appid", env.getProperty("APP_ID"));       //公众号id
        paramMap.put("mch_id", env.getProperty("MCH_ID"));     // 商户号
        paramMap.put("nonce_str", WXPayUtil.generateNonceStr());    //生成随机字符串
        paramMap.put("total_fee", "1");        //测试金额，单位为分,非测试为totalAmount*100
        paramMap.put("trade_type", "APP");    //支付类型
        paramMap.put("notify_url", env.getProperty("NOTIFYURL"));  //通知地址
        paramMap.put("out_trade_no", UuidUtil.getUUID());       //商户订单号
        paramMap.put("body", "乡村旅游定金支付");
        paramMap.put("spbill_create_ip", "127.0.0.1");

        return null;
    }

    @Autowired
    Session session;

    @ApiOperation(value = "用户端-余额支付")
    @RequestMapping(value = "/balancePay", method = {RequestMethod.POST, RequestMethod.GET})
    public Result balancePay(@ApiParam(name = "orderId", value = "订单Id") String orderId,
                             @ApiParam(name = "type", value = "类型 1.商品订单 2.快递跑腿") Integer type) throws Exception {
        switch (type) {
            case 1:
                //商品订单支付
                String[] ids = orderId.split(",");
                List<UserOrder> userOrders = userOrderService.selectBatchIds(Arrays.asList(ids));
                userOrderService.balancePay(userOrders, session.lUserId(), session.getCityId());
                break;
            case 2:
                //快递跑腿订单支付
                expressService.balancePay(orderId, session.lUserId());
                break;
        }
        //余额支付
        return new Result().success("余额支付成功!");
    }


    @ApiOperation(value = "用户订单-支付完成")
    @RequestMapping(value = "/complete", method = {RequestMethod.POST, RequestMethod.GET})
    public Result complete(@ApiParam(name = "orderId", value = "订单Id") String orderId) {
        String[] ids = orderId.split(",");
        List<UserOrder> userOrders = userOrderService.selectBatchIds(Arrays.asList(ids));
        return new Result().success(userOrderService.payComplete(userOrders));
    }

    /*   @ApiOperation(value = "用户端-其他支付")
       @PostMapping(value = "/payStore")
       public Result payStore(@ApiParam(name = "price", value = "支付金额") String price,
                              @ApiParam(name = "userId", value = "用户Id") Integer userId,
                              @ApiParam(name = "payType", value = "支付方式-2支付宝-3微信公众号-4微信APP-5银行卡") Integer payType,
                              @ApiParam(name = "subject", value = "支付标题") String subject) throws UnsupportedEncodingException, ChannelException {

           return new Result().success("", userOrderService.payStore(price, payType, userId, subject));
       }
   */
    @ApiOperation(value = "用户端-到店支付-获取店铺名字")
    @GetMapping(value = "/getShopName")
    public Result getShopName(String shopId) {
        return new Result().success("", shopService.selectById(shopId).getShopName());

    }

 /*   @ApiOperation(value = "用户端-到店支付-支付完成")
    @RequestMapping(value = "/payStoreComplete", method = {RequestMethod.POST, RequestMethod.GET})
    public Result payStoreComplete(@ApiParam(name = "shopId", value = "商家Id") Integer shopId,
                                   @ApiParam(name = "price", value = "支付金额") String price) {
        userOrderService.payStoreComplete(shopId, price);
        return new Result().success();
    }*/

/*    @ApiOperation(value = "会员充值")
    @PostMapping(value = "/memberCharge")
    public Result memberCharge(@ApiParam(name = "userId", value = "用户Id") Integer userId,
                               @ApiParam(name = "memberId", value = "会员配置Id") Integer memberId,
                               @ApiParam(name = "payType", value = "支付方式-2支付宝-3微信公众号-4微信APP-5银行卡") Integer payType) throws UnsupportedEncodingException, ChannelException {

        return new Result().success(userRemainderService.memberCharge(userId, memberId, payType));
    }*/

/*    @ApiOperation(value = "会员充值-支付完成")
    @PostMapping(value = "/memberChargeComplete")
    public Result memberChargeComplete(@ApiParam(name = "remainderId", value = "用户余额Id") Integer remainderId,
                                       @ApiParam(name = "rechargeRecordId", value = "交易记录Id") Integer rechargeRecordId,
                                       @ApiParam(name = "status", value = "支付状态 0失败 1成功") Integer status) {
        userRemainderService.chargeComplete(remainderId, rechargeRecordId, status);
        return new Result().success();
    }*/

    /**
     * 微信支付获取config
     */
    @ApiOperation("微信公众号支付获取config")
    @ApiImplicitParams(value = {
        @ApiImplicitParam(name = "url", value = "生成url"),
    })
    @RequestMapping(value = "/ticket", method = {RequestMethod.POST, RequestMethod.GET})
    public Result jsapiTicket(@RequestParam String url) throws Exception {
        String timestamp = System.currentTimeMillis() / 1000L + "";
        String ticket = JsapiTicketUtil.JsapiTicket2();
//        String ss = "jsapi_ticket=&noncestr=Wm3WZYTPz0wzccnW&timestamp=1414587457&url=http://mp.weixin.qq.com?params=value";
        String randomString = WxPay.getRandomString(16);
        String str = "jsapi_ticket=" + ticket + "&noncestr=" + randomString + "&timestamp=" + timestamp + "&url=" + url;
        String signature = SHA1.SHA1(str);
        Map<String, String> map = new HashMap();
        map.put("timestamp", timestamp);
        map.put("ticket", ticket);
        map.put("noncestr", randomString);
        map.put("signature", signature);
        map.put("appId", JsapiTicketUtil.APP_ID);
        return new Result().success(map);
    }

    @ApiOperation(value = "获取openId")
    @GetMapping(value = "/getOpenId")
    public Result getCode(Integer userId, String code) throws UnsupportedEncodingException, ChannelException {
        User user = new User();
        user.setId(userId);
        user.setOpenId(WxPubOAuthUtil.getOpenid(code));
        user.updateById();
        return new Result().success();
    }
}
