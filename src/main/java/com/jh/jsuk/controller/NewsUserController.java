package com.jh.jsuk.controller;


import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 用户消息表 前端控制器
 * </p>
 *
 * @author lpf
 * @since 2018-06-20
 */
@RestController
@RequestMapping(value = "/newsUser", method = {RequestMethod.POST, RequestMethod.GET})
public class NewsUserController {

}

