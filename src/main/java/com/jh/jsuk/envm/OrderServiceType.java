package com.jh.jsuk.envm;

import com.github.tj123.common.enum2md.Envm;
import lombok.Getter;

/**
 * 订单回退类型
 * 1=仅退款,2=退货退款,3=换货
 */
@Getter
@Envm(name = "售后类型")
public enum OrderServiceType {

    RETURN_MONEY(1, "退款"),

    RETURN_GOODS(2, "退货退款"),

    CHANGE_GOODS(3, "换货");

    private final Integer key;

    private final String value;

    OrderServiceType(Integer key, String value) {
        this.key = key;
        this.value = value;
    }

}
