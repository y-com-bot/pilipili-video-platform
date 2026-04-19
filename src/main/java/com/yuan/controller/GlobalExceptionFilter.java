package com.yuan.controller;

import com.yuan.utils.ExceptionHandler;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class GlobalExceptionFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(req, resp);
        } catch (Throwable e) {
            ExceptionHandler.handle(e, (HttpServletRequest) req, (HttpServletResponse) resp);
        }
    }
}
