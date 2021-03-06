package com.jh.jsuk.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.jh.jsuk.entity.DistributionDetail;
import com.jh.jsuk.service.DistributionDetailService;
import com.jh.jsuk.utils.Result;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lpf
 * @since 2018-06-26
 */
@RestController
@RequestMapping("/distributionDetail")
public class DistributionDetailController {

    @Autowired
    private DistributionDetailService distributionDetailService;

    @ApiOperation("骑手-骑手端我的收入明细")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "current", value = "当前页码",
                    required = false, paramType = "query", dataType = "integer"),
            @ApiImplicitParam(name = "size", value = "每页条数",
                    required = false, paramType = "query", dataType = "integer"),
            @ApiImplicitParam(name = "startDate", value = "开始日期 格式：yyyy-MM-dd 为空查所有",
                    required = false, paramType = "query", dataType = "integer"),
            @ApiImplicitParam(name = "stopDate", value = "结束日期 格式：yyyy-MM-dd",
                    required = false, paramType = "query", dataType = "integer"),
    })
    @GetMapping("/list")
    public Result list(Page page,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String stopDate,
                       Integer userId) {
        Page detailPage;
        if (StrUtil.isNotBlank(startDate) && StrUtil.isNotBlank(stopDate)) {
            String startTime = startDate + " 00:00:00";
            String endTime = stopDate + " 23:59:59";
            detailPage = distributionDetailService.selectPage(page, new EntityWrapper<DistributionDetail>()
                    .eq(DistributionDetail.USER_ID, userId)
                    .between(DistributionDetail.PUBLISH_TIME, startTime, endTime)
                    .orderBy(DistributionDetail.PUBLISH_TIME, false));
        } else {
            detailPage = distributionDetailService.selectPage(page, new EntityWrapper<DistributionDetail>()
                    .eq(DistributionDetail.USER_ID, userId)
                    .orderBy(DistributionDetail.PUBLISH_TIME, false));
        }
        return new Result().success(detailPage);
    }

}

