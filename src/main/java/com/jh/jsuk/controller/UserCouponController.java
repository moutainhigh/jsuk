package com.jh.jsuk.controller;


import com.jh.jsuk.entity.UserCoupon;
import com.jh.jsuk.service.UserCouponService;
import com.jh.jsuk.utils.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 用户优惠券 前端控制器
 * </p>
 *
 * @author xuchuruo
 * @since 2018年5月23日
 */
@Api(tags = {"优惠券:"})
@RestController
@RequestMapping(value = "/userCoupon", method = {RequestMethod.POST, RequestMethod.GET})
public class UserCouponController {

    @Autowired
    private UserCouponService userCouponService;


    //用户-我的-优惠券-优惠券总数
    @ApiOperation("用户-优惠券总数")
    @PostMapping("/getCount")
    public Result getCount(@RequestParam @ApiParam(value = "用户ID", required = true) Integer userId) {
        UserCoupon userCoupon = userCouponService.selectById(userId);
        return new Result().success(userCoupon);
    }

    @ApiOperation("用户-查询优惠券")
    @PostMapping("/getUserCoupon")
    public Result getUserCoupon(@RequestParam @ApiParam(value = "用户ID", required = true) Integer userId) {
        UserCoupon userCoupon = userCouponService.selectById(userId);
        return new Result().success(userCoupon);
    }

}
