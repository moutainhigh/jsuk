package com.jh.jsuk.envm;

/**
 * Author:谢英亮
 * Date:2018/7/24 11:35
 * Description:支付类型
 */
public enum PayType {

    BALANCE_PAY(0, "余额"),

    PAY_ON_DELIVERY(1, "货到付款"),

    ALI_PAY(2, "支付宝"),

    WECHAT_PAY(3, "微信");

    private final int key;

    private final String value;

    PayType(int key, String value) {
        this.key = key;
        this.value = value;
    }

    public int getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
