package com.hmdp.controller;

import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送短信验证码：
     * 1. 接收前端传入的手机号
     * 2. 调用 service 校验手机号并生成验证码
     * 3. 将验证码保存到 Redis
     * 4. 返回处理结果
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone) {
        return userService.sendCode(phone);
    }

    /**
     * 登录流程：
     * 1. 接收手机号和验证码
     * 2. 调用 service 校验验证码
     * 3. 登录成功后生成 token 并写入 Redis
     * 4. 将 token 返回给前端
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm) {
        return userService.login(loginForm);
    }

    /**
     * 退出登录：
     * 1. 从请求头中获取 token
     * 2. 根据 token 删除 Redis 中的登录态
     * 3. 完成本次退出操作
     * 4. 返回成功结果
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        return userService.logout(request.getHeader("authorization"));
    }

    /**
     * 获取当前登录用户：
     * 1. 前置拦截器先把用户信息从 Redis 恢复出来
     * 2. 再放入 UserHolder
     * 3. 这里直接从 ThreadLocal 中取当前用户
     * 4. 返回给前端
     */
    @GetMapping("/me")
    public Result me() {
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    /**
     * 查询用户详情信息，这里查询的是用户扩展资料。
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId) {
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        return Result.ok(info);
    }
}
