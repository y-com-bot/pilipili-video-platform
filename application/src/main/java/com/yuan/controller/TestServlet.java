package com.yuan.controller;

import springMVC.MyController;
import springMVC.MyRequestMapping;
import springMVC.MyResponseBody;

@MyController
@MyRequestMapping("/api/system")
public class TestServlet {

    @MyRequestMapping("/ping")
    @MyResponseBody
    public String ping() {
        return "{\"code\":200,\"success\":true,\"message\":\"pong\"}";
    }
}
