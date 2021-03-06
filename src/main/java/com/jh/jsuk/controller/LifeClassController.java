package com.jh.jsuk.controller;


import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.jh.jsuk.entity.*;
import com.jh.jsuk.entity.vo.ActivityVo;
import com.jh.jsuk.entity.vo.MarketCommentVo;
import com.jh.jsuk.service.*;
import com.jh.jsuk.utils.Result;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 便捷生活分类 前端控制器
 * </p>
 *
 * @author lpf
 * @since 2018-06-20
 */
@Api(tags = {"便捷生活相关API"})
@RestController
@RequestMapping("/lifeClass")
public class LifeClassController {

    @Autowired
    private ActivityService activityService;
    @Autowired
    private LifeClassService lifeClassService;
    @Autowired
    private CarService carService;
    @Autowired
    private BannerService bannerService;
    @Autowired
    private MarketCommentService marketCommentService;
    @Autowired
    private ActivityTransactionAreaService activityTransactionAreaService;

    @ApiOperation("用户-便捷生活-获取banner/商品列表")
    @RequestMapping(value = "/getLifeInfo", method = {RequestMethod.POST, RequestMethod.GET})
    public Result getLifeInfo() {
        // 封装结果map
        Map<String, Object> map = new HashMap<>();
        /**
         * 获取banner
         */
        List<Banner> bannerList = bannerService.selectList(new EntityWrapper<Banner>()
                // 6=便捷生活
                .eq(Banner.BANNER_LOCATION, 6)
                .eq(Banner.IS_VALID, 1)
                .orderBy(Banner.SORT, false));
        map.put("banner", bannerList);
        /**
         * 获取分类
         */
        List<LifeClass> lifeClassList = lifeClassService.selectList(new EntityWrapper<>());
        map.put("classLife", lifeClassList);
        return new Result().success(map);
    }

/*    @ApiOperation("用户-便捷生活-获取分类列表")
    @RequestMapping(value = "/findLifeClass", method = {RequestMethod.POST, RequestMethod.GET})
    public Result findLifeClass() {
        List<LifeClass> lifeClassList = lifeClassService.selectList(new EntityWrapper<>());
        return new Result().success(lifeClassList);
    }*/
    @ApiOperation("用户-便捷生活-车辆列表")
    @RequestMapping(value = "/getCarList", method = {RequestMethod.POST, RequestMethod.GET})
    public Result getCarList() {
        List<Car> carList = carService.selectList(new EntityWrapper<Car>()
                .orderBy(Car.RANK, false));
        if (CollectionUtils.isEmpty(carList)) {
            return new Result().success(null);
        }
        return new Result().success(carList);
    }

    @ApiOperation("用户-便捷生活-根据分类选择车辆")
    @RequestMapping(value = "/getCarByClassId", method = {RequestMethod.POST, RequestMethod.GET})
    public Result getCarByClassId(@RequestParam Integer classId) {
        List<Car> carList = carService.selectList(new EntityWrapper<Car>()
                .eq(Car.CLASS_ID, classId)
                .orderBy(Car.RANK, false));
        if (CollectionUtils.isEmpty(carList)) {
            return new Result().success(null);
        }
        return new Result().success(carList);
    }

    @ApiOperation("便捷生活-首页婚车列表")
    @RequestMapping(value = "/getActivityByClassId", method = {RequestMethod.POST, RequestMethod.GET})
    public Result getActivityByClassId(@RequestParam Integer classId, @RequestParam Integer ModularId) {
        // 封装数据map
        ModularId=ModularId==null?7:ModularId;
        Map<String, Object> map = new HashMap<>();

        Page demandPage = activityService.selectPage(
                new Page<>(1, 3),
                new EntityWrapper<Activity>()
                        .eq(Activity.MODULAR_ID,ModularId)
                        .eq(Activity.IS_DEL, 0)
                        .eq(Activity.CLASS_ID,classId)
                        .orderBy(Activity.RANK, false));
        map.put("list", demandPage.getRecords());

        return new Result().success(map);
    }

    @ApiOperation("用户-便捷生活-更多共享婚车商品列表")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "current", value = "当前页码",
                    paramType = "query", dataType = "integer"),
            @ApiImplicitParam(name = "size", value = "每页条数",
                    paramType = "query", dataType = "integer"),
    })
    @RequestMapping(value = "/getMoreActivity", method = {RequestMethod.POST, RequestMethod.GET})
    public Result getMoreActivity(Page page,
                                  Integer ModularId) {
            ModularId=ModularId==null?7:ModularId;
            Page providePage = activityService.selectPage(page, new EntityWrapper<Activity>()
                    .eq(Activity.MODULAR_ID,ModularId)
                    .eq(Activity.IS_DEL, 0)
                    .orderBy(Activity.RANK, false));

            return new Result().success(providePage);
    }

    @ApiOperation("用户-便捷生活-根据分类ID查询商品列表")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "current", value = "当前页码",
                    paramType = "query", dataType = "integer"),
            @ApiImplicitParam(name = "size", value = "每页条数",
                    paramType = "query", dataType = "integer"),
    })
    @RequestMapping(value = "/getActivityListByClassId", method = {RequestMethod.POST, RequestMethod.GET})
    public Result getActivityListByClassId(Page page,
                                           Integer ModularId,
                                           @RequestParam Integer classId) {
        ModularId=ModularId==null?7:ModularId;
        Page activityPage = activityService.selectPage(page, new EntityWrapper<Activity>()
                .eq(Activity.MODULAR_ID,ModularId)
                .eq(Activity.CLASS_ID, classId)
                .eq(Activity.IS_DEL, 0)
                .orderBy(Activity.RANK, false));
        return new Result().success(activityPage);
    }

    @ApiOperation("用户-便捷生活-根据商品ID查询商品详细信息")
    @RequestMapping(value = "/getActivityById", method = {RequestMethod.POST, RequestMethod.GET})
    public Result getActivityById(@RequestParam Integer id) {
        Activity activity=activityService.selectById(id);
        /**
         * 判断活动是普通活动还是共享婚车活动
         * 共享婚车活动需要查出活动的车辆信息
         */
        ActivityVo activityVo=null;
        if( activity.getCarId()==null) {    //普通活动
             activityVo = activityService.findActivityById(id);
        }
        if( activity.getCarId()!=null){   //共享婚车活动
            activityVo=activityService.selectMerryageCar(id);
        }
        return new Result().success(activityVo);
    }

    @ApiOperation("用户-便捷生活-根据活动ID查询留言内容/总数")
    @RequestMapping(value = "/getComment", method = {RequestMethod.POST, RequestMethod.GET})
    public Result getComment(@RequestParam Integer activityId) {
        // 封装结果map
        Map<String, Object> map = new HashMap<>();
        int count = marketCommentService.selectCount(new EntityWrapper<MarketComment>()
                .eq(MarketComment.ACTIVITY_ID, activityId));
        map.put("count", count);
/*        List<MarketComment> marketCommentList = marketCommentService.selectList(new EntityWrapper<MarketComment>()
                .eq(MarketComment.ACTIVITY_ID, activityId)
                .orderBy(MarketComment.PUBLISH_TIME, false));
        map.put("comment", marketCommentList);*/
        List<MarketCommentVo> marketCommentList=marketCommentService.selectMarketCommentVoList(activityId);
        map.put("comment", marketCommentList);

        return new Result().success(map);
    }

    @ApiOperation("用户-便捷生活-交易区域选择")
    @RequestMapping(value = "/chooseArea", method = {RequestMethod.POST, RequestMethod.GET})
    public Result chooseArea() {
        List<ActivityTransactionArea> areaList = activityTransactionAreaService.selectList(new EntityWrapper<>());
        if (CollectionUtils.isEmpty(areaList)) {
            return new Result().success(null);
        } else {
            return new Result().success(areaList);
        }
    }


    @ApiOperation("便捷生活-发布-分类")
    @RequestMapping(value="/findLifeClass",method={RequestMethod.GET,RequestMethod.POST})
    public Result findLifeClass(Page page){
        Page<LifeClass> lifeClassPage=lifeClassService.selectPage(page,new EntityWrapper<LifeClass>());
        return new Result().success(lifeClassPage);
    }

}

