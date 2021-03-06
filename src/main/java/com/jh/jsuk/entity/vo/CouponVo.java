package com.jh.jsuk.entity.vo;

import com.jh.jsuk.entity.Coupon;
import com.jh.jsuk.entity.UserCoupon;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CouponVo extends Coupon {

    private UserCoupon userCoupon;

    private Boolean isTimeOut;

}
