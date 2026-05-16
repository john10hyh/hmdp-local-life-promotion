package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 刷新 token 拦截器：
 * 主要负责恢复用户登录态，并在用户活跃时刷新 Redis 中的 TTL。
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从请求头中获取 token，没有 token 则说明本次请求可能是匿名访问。
        String token = request.getHeader("authorization");
        if (token == null || token.trim().isEmpty()) {
            return true;
        }

        // 2. 根据 token 到 Redis 查询用户登录态。
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(tokenKey);
        if (userMap == null || userMap.isEmpty()) {
            return true;
        }

        // 3. 若 Redis 中有用户信息，则转换成 UserDTO 并放入 ThreadLocal。
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        UserHolder.saveUser(userDTO);

        // 4. 刷新 token 的有效期，形成“滑动过期”效果。
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后一定要清理 ThreadLocal，避免线程复用造成用户信息串用。
        UserHolder.removeUser();
    }
}
