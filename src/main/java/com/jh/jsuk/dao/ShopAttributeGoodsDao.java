package com.jh.jsuk.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.jh.jsuk.entity.ShopAttributeGoods;
import com.jh.jsuk.entity.vo.ShopAttributeVo;

/**
 * <p>
 * 店铺分类属性关联商品表 Mapper 接口
 * </p>
 *
 * @author xcr
 * @since 2018-06-21
 */
public interface ShopAttributeGoodsDao extends BaseMapper<ShopAttributeGoods> {

    ShopAttributeVo getShopAttributeByShopId(Integer shopId);
}
