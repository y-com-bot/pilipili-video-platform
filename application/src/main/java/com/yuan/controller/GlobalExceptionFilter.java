package com.yuan.controller;

import com.yuan.exception.AppException;
import com.yuan.utils.JsonUtils;
import framework.springMVC.MyControllerAdvice;
import framework.springMVC.MyExceptionHandler;
import framework.springMVC.MyResponseBody;

/**
 * 这里虽然类名还叫 GlobalExceptionFilter，
 * 但角色已经改成 framework.springMVC 的全局异常处理器。
 *
 * 我保留了原文件名，这样你在项目历史里更容易看出它是如何演化的。
 */
@MyControllerAdvice
public class GlobalExceptionFilter {

    @MyExceptionHandler(AppException.class)
    @MyResponseBody
    public String handleAppException(AppException e) {
        return JsonUtils.error(e.getCode(), e.getMessage());
    }

    @MyExceptionHandler(Exception.class)
    @MyResponseBody
    public String handleException(Exception e) {
        return JsonUtils.error(500, "系统开小差了，请稍后再试");
    }
}
