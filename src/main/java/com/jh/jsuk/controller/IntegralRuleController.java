package com.jh.jsuk.controller;


import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.jh.jsuk.entity.IntegralRule;
import com.jh.jsuk.service.IntegralRuleService;
import com.jh.jsuk.utils.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 积分抵扣规则 前端控制器
 * </p>
 *
 * @author tj
 * @since 2018-07-19
 */
@Api(tags = "积分抵扣规则")
@RestController
@RequestMapping("/integralRule")
public class IntegralRuleController {

    @Autowired
    IntegralRuleService integralRuleService;

    @ApiOperation("积分抵扣规则")
    @GetMapping
    public R integralRule(Integer shopId){
        return R.succ(integralRuleService.selectOne(new EntityWrapper<>()));
    }

    @PatchMapping
    public R edit(IntegralRule integralRule){
        IntegralRule rule = integralRuleService.selectById(1);
        if(rule == null){
            integralRule.setId(1);
            integralRule.insert();
        }else {
            integralRule.setId(1);
            integralRule.updateById();
        }
        return R.succ();
    }


}

