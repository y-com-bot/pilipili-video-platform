package com.yuan.controller;

import framework.springMVC.MyController;
import framework.springMVC.MyRequestMapping;
import framework.springMVC.MyResponseBody;

@MyController
@MyRequestMapping("/api/system")
public class TestServlet {

    @MyRequestMapping("/ping")
    @MyResponseBody
    public String ping() {
        return "{\"code\":200,\"success\":true,\"message\":\"pong\"}";
    }
}
