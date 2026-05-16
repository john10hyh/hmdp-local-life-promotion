package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;

/**
 * 用户服务接口，负责登录相关核心能力。
 */
public interface IUserService extends IService<User> {

    /**
     * 发送短信验证码。
     */
    Result sendCode(String phone);

    /**
     * 使用手机号和验证码完成登录。
     */
    Result login(LoginFormDTO loginForm);

    /**
     * 根据 token 退出登录。
     */
    Result logout(String token);
}
