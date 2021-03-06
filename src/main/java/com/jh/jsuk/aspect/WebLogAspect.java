package com.jh.jsuk.aspect;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Web层日志切面
 */
@Slf4j
@Aspect
@Order(5)
@Component
public class WebLogAspect {

    ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Pointcut("execution(public * com.jh.jsuk.controller.*..*(..))")
    public void webLog() {
    }

    @Before("webLog()")
    public void doBefore(JoinPoint joinPoint) throws IOException {
        startTime.set(System.currentTimeMillis());

        // 接收到请求，记录请求内容
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

//        log.info("jwt值:{}",request.getHeader(Constant.JWT_HEADER));

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String element = headerNames.nextElement();
            log.info("{} -> {}", element, request.getHeader(element));
        }

        // 记录下请求内容
        log.info("地址 : {}", request.getRequestURL().toString());
        log.info("请求方式 : {}", request.getMethod());
        log.info("IP : {}", request.getRemoteAddr());
        Signature signature = joinPoint.getSignature();
        String declaringTypeName = signature.getDeclaringTypeName();
        String methodName = signature.getName();
        Matcher matcher = Pattern.compile("\\w+$").matcher(declaringTypeName);
        String fileName = matcher.find() ? matcher.group() : "";
        System.out.println(StrUtil.format("{}.{}({}:{})",
            declaringTypeName, methodName, fileName, 100));
//        log.info("执行的方法: {}.{}({}:{})", declaringTypeName, methodName, fileName, 21);
        log.info("参数 : {}", Arrays.toString(joinPoint.getArgs()));
    }

    @AfterReturning(returning = "ret", pointcut = "webLog()")
    public void doAfterReturning(Object ret) {
        // 处理完请求，返回内容
        try {
//            log.info("返回内容 : {}", new ObjectMapper().writeValueAsString(ret));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        log.info("花费时间 : " + (System.currentTimeMillis() - startTime.get()) + "毫秒");
    }


}
