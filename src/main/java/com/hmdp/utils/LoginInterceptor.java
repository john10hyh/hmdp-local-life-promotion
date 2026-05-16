package com.hmdp.utils;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器：
 * 专门负责拦截必须登录后才能访问的接口。
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从 ThreadLocal 中获取当前登录用户。
        // 2. 若为空，说明用户未登录，直接返回 401。
        // 3. 若不为空，说明前置拦截器已经完成登录态恢复。
        // 4. 放行后续业务处理。
        if (UserHolder.getUser() == null) {
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
