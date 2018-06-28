package com.jh.jsuk.entity;

import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * <p>
 * 订单申请售后
 * </p>
 *
 * @author lpf
 * @since 2018-06-28
 */
@TableName("js_user_order_service")
public class UserOrderService extends Model<UserOrderService> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 订单ID
     */
    @ApiModelProperty(name = "订单ID", required = true, value = "orderId")
    private Integer orderId;
    /**
     * 退款原因
     */
    @ApiModelProperty(name = "退货原因", required = true, value = "refundReason")
    private String refundReason;
    /**
     * 售后类型,1=仅退款,2=退货退款,3=换货
     */
    @ApiModelProperty(name = "售后类型,1=仅退款,2=退货退款,3=换货", required = true, value = "type")
    private Integer type;
    /**
     * 具体原因
     */
    @ApiModelProperty(name = "订单ID", value = "content")
    private String content;
    /**
     * 图片
     */
    @ApiModelProperty(name = "订单ID", value = "image")
    private String image;
    /**
     * 退款金额
     */
    @ApiModelProperty(name = "订单ID", required = true, value = "price")
    private String price;
    /**
     * 规格ID
     */
    @ApiModelProperty(name = "规格ID,换货", value = "sizeId")
    private Integer sizeId;
    /**
     * 地址ID
     */
    @ApiModelProperty(name = "地址ID,换货", value = "addressId")
    private Integer addressId;

    public Integer getAddressId() {
        return addressId;
    }

    public void setAddressId(Integer addressId) {
        this.addressId = addressId;
    }

    public Integer getSizeId() {
        return sizeId;
    }

    public void setSizeId(Integer sizeId) {
        this.sizeId = sizeId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public static final String ID = "id";

    public static final String ORDER_ID = "order_id";

    public static final String REFUND_REASON = "refund_reason";

    public static final String TYPE = "type";

    public static final String CONTENT = "content";

    public static final String IMAGE = "image";

    public static final String PRICE = "price";

    public static final String SIZE_ID = "size_id";

    public static final String ADDRESS_ID = "address_id";

    @Override
    protected Serializable pkVal() {
        return this.id;
    }

    @Override
    public String toString() {
        return "UserOrderService{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", refundReason='" + refundReason + '\'' +
                ", type=" + type +
                ", content='" + content + '\'' +
                ", image='" + image + '\'' +
                ", price='" + price + '\'' +
                ", sizeId=" + sizeId +
                ", addressId=" + addressId +
                '}';
    }
}