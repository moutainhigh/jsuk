package com.jh.jsuk.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.jh.jsuk.entity.WeatherCity;

import java.util.List;

/**
 * <p>
 * 城市天气 Mapper 接口
 * </p>
 *
 * @author lpf
 * @since 2018-06-20
 */
public interface WeatherCityDao extends BaseMapper<WeatherCity> {

    List<WeatherCity> getOpenCityList();

    List<WeatherCity> getOpenCityList2();
}
