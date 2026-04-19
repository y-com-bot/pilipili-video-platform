package com.yuan.controller;

import com.yuan.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
        private UserService userService = new UserService();
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if(username == null || password == null){
            resp.getWriter().write("用户名或密码不能为空");
            return;
        }

        String resultMessage = userService.login(username, password);

        if(resultMessage.startsWith("SUCCESS")) {
            resp.getWriter().write(resultMessage);
        }else{
            resp.getWriter().write(resultMessage);
        }
    }
}