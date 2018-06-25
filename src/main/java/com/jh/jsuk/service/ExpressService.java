package com.jh.jsuk.service;

import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.IService;
import com.jh.jsuk.entity.Express;

/**
 * <p>
 * 快递跑腿 服务类
 * </p>
 *
 * @author lpf
 * @since 2018-06-20
 */
public interface ExpressService extends IService<Express> {

    Page getExpressListBy(Page page, Wrapper wrapper, Integer status, Integer type, Integer userId);

}
