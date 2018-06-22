package com.jh.jsuk.controller;


import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.jh.jsuk.entity.Shop;
import com.jh.jsuk.entity.ShopGoodsSize;
import com.jh.jsuk.entity.vo.GoodsSalesPriceVo;
import com.jh.jsuk.entity.vo.ModularPortalVo;
import com.jh.jsuk.service.ModularPortalService;
import com.jh.jsuk.service.ShopGoodsService;
import com.jh.jsuk.service.ShopGoodsSizeService;
import com.jh.jsuk.service.ShopService;
import com.jh.jsuk.utils.MyEntityWrapper;
import com.jh.jsuk.utils.Result;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 模块分类 前端控制器
 * </p>
 *
 * @author xuchuruo
 * @since 2018-06-20
 */
@Api(tags = {"模块相关API--(便捷生活/乡村旅游/二手市场不需要调这个):"})
@RestController
@RequestMapping("/modularPortal")
public class ModularPortalController {

    @Autowired
    private ModularPortalService modularPortalService;
    @Autowired
    private ShopGoodsService shopGoodsService;
    @Autowired
    private ShopService shopService;
    @Autowired
    private ShopGoodsSizeService shopGoodsSizeService;

/*    @ApiOperation("用户端-根据模块ID获取商品/店铺列表")
    @PostMapping("/getByModular")
    public Result getByModular(@ApiParam(value = "模块ID", required = true) Integer modularId) {
        // 封装数据map
        Map<String, Object> map = MapUtil.newHashMap();
        *//**
     * 商品推荐
     *//*
        Page<ShopGoods> shopGoodsPage = shopGoodsService.selectPage(
                new Page<>(1, 4),
                new EntityWrapper<ShopGoods>()
                        // 商品状态.0-待审核 1-在售 2-下架
                        .eq(ShopGoods.STATUS, 1)
                        // 是否推荐,0=不推荐,1=推荐
                        .eq(ShopGoods.IS_RECOMMEND, 1)
                        .eq(ShopGoods.SHOP_MODULAR_ID, modularId)
                        .orderBy(ShopGoods.SALE_AMONT, false));
        map.put("shopGoods", shopGoodsPage.getRecords());
        */

    /**
     * 店铺列表
     *//*
        Page<Shop> shopPage = shopService.selectPage(
                new Page<>(1, 3),
                new EntityWrapper<Shop>()
                        // 是否可用  0不可用 1可用
                        .eq(Shop.CAN_USE, 1)
                        // 是否推荐,0=不推荐,1=推荐
                        .eq(Shop.IS_RECOMMEND, 1)
                        .eq(Shop.MODULAR_ID, modularId)
                        .orderBy(Shop.TOTAL_VOLUME, false));
        map.put("shop", shopPage.getRecords());
        return new Result().success();
    }*/
    @ApiOperation(value = "用户端-根据模块ID获取店铺/商品列表")
    @GetMapping("/getShopAndGoodsByModular")
    public Result getShopAndGoodsByModular(@ApiParam(value = "模块ID", required = true) Integer modularId) {
        // 封装数据map
        Map<String, Object> map = MapUtil.newHashMap();
        /**
         * 店铺列表
         */
        Page<Shop> shopPage = shopService.selectPage(
                new Page<>(1, 3),
                new EntityWrapper<Shop>()
                        // 是否可用  0不可用 1可用
                        .eq(Shop.CAN_USE, 1)
                        // 是否推荐,0=不推荐,1=推荐
                        .eq(Shop.IS_RECOMMEND, 1)
                        .eq(Shop.MODULAR_ID, modularId)
                        .orderBy(Shop.TOTAL_VOLUME, false));
        if (CollectionUtils.isEmpty(shopPage.getRecords())) {
            map.put("shop", null);
        } else {
            map.put("shop", shopPage.getRecords());
        }
        /**
         * 商品列表
         */
        // 商品数据
        List<GoodsSalesPriceVo> goodsSalesPriceVos = shopGoodsService.findShopGoodsByModularId(modularId);
        if (CollectionUtils.isEmpty(goodsSalesPriceVos)) {
            map.put("shopGoods", null);
        } else {
            map.put("shopGoods", goodsSalesPriceVos);
        }
        return new Result().success(map);
    }

    @ApiOperation(value = "用户端-根据模块ID获取更多店铺", notes = "modularId=0 -->获取首页更多精选商家列表")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "current", value = "当前页码",
                    paramType = "query", dataType = "integer"),
            @ApiImplicitParam(name = "size", value = "每页条数",
                    paramType = "query", dataType = "integer"),
    })
    @GetMapping("/shopListByModularId")
    public Result shopListByModularId(Page page, @ApiParam(value = "模块ID", required = true) Integer modularId) {
        if (modularId == 0) {
            // 首页更多精选商家
            Page shopPage = shopService.selectPage(page, new EntityWrapper<Shop>()
                    // 是否可用  0不可用 1可用
                    .eq(Shop.CAN_USE, 1)
                    // 是否推荐,0=不推荐,1=推荐
                    .eq(Shop.IS_RECOMMEND, 1)
                    .eq(Shop.MODULAR_ID, modularId)
                    .orderBy(Shop.TOTAL_VOLUME, false));
            return new Result().success(shopPage);
        } else {
            // 更多商家列表
            Page shopPage = shopService.selectPage(page, new EntityWrapper<Shop>()
                    .eq(Shop.CAN_USE, 1)
                    .eq(Shop.MODULAR_ID, modularId)
                    .orderBy(Shop.TOTAL_VOLUME, false));
            return new Result().success(shopPage);
        }
    }

    @ApiOperation(value = "用户端-根据模块ID获取更多商品")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "current", value = "当前页码",
                    paramType = "query", dataType = "integer"),
            @ApiImplicitParam(name = "size", value = "每页条数",
                    paramType = "query", dataType = "integer"),
    })
    @GetMapping("/shopGoodsListByModularId")
    public Result shopGoodsListByModularId(Page page, @ApiParam(value = "模块ID", required = true) Integer modularId) {
        MyEntityWrapper<ShopGoodsSize> ew = new MyEntityWrapper<>();
        Page goodsSizeVoList = shopGoodsService.shopGoodsListByModularId(page, ew, modularId);
        return new Result().success(goodsSizeVoList);
    }

    @ApiOperation(value = "商家端-获取分类列表")
    @GetMapping("/getModularList")
    public Result getModularList() {
        List<ModularPortalVo> modularPortalVoList = modularPortalService.getModularList();
        return new Result().success(modularPortalVoList);
    }

}

