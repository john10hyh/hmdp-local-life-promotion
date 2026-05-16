package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送验证码流程：
     * 1. 校验手机号格式是否正确
     * 2. 生成 6 位随机验证码
     * 3. 以 login:code:手机号 为 key 写入 Redis
     * 4. 设置 TTL，防止验证码长期占用内存
     */
    @Override
    public Result sendCode(String phone) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(
                RedisConstants.LOGIN_CODE_KEY + phone,
                code,
                RedisConstants.LOGIN_CODE_TTL,
                TimeUnit.MINUTES
        );
        // 开发阶段先打印验证码，后续接短信平台时替换这里即可。
        log.debug("发送短信验证码成功，手机号：{}，验证码：{}", phone, code);
        return Result.ok();
    }

    /**
     * 登录流程：
     * 1. 校验手机号格式和验证码是否正确
     * 2. 根据手机号查询用户，不存在则自动创建新用户
     * 3. 生成随机 token，作为当前登录会话凭证
     * 4. 将用户简要信息写入 Redis Hash，并返回 token
     */
    @Override
    public Result login(LoginFormDTO loginForm) {
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        if (cacheCode == null || !cacheCode.equals(loginForm.getCode())) {
            return Result.fail("验证码错误");
        }

        // 先根据手机号查用户，若为空则执行自动注册。
        User user = query().eq("phone", phone).one();
        if (user == null) {
            user = createUserWithPhone(phone);
        }

        // 使用随机 token 标识当前会话，便于前端后续放入请求头中传递。
        String token = UUID.randomUUID().toString(true);
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);

        // 将用户对象转为 Map，写入 Redis Hash，字段级存储更方便读取和续期。
        Map<String, Object> userMap = BeanUtil.beanToMap(
                userDTO,
                new HashMap<>(8),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? null : fieldValue.toString())
        );
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 验证码只允许一次性使用，登录成功后立刻删除。
        stringRedisTemplate.delete(RedisConstants.LOGIN_CODE_KEY + phone);
        return Result.ok(token);
    }

    /**
     * 退出登录流程：
     * 1. 获取前端传入的 token
     * 2. 拼接 Redis 中的登录态 key
     * 3. 删除 token 对应的用户会话
     * 4. 返回成功结果
     */
    @Override
    public Result logout(String token) {
        if (token != null && !token.trim().isEmpty()) {
            stringRedisTemplate.delete(RedisConstants.LOGIN_USER_KEY + token);
        }
        return Result.ok();
    }

    /**
     * 自动注册流程：
     * 1. 用手机号创建用户对象
     * 2. 生成默认昵称
     * 3. 保存到数据库
     * 4. 返回新创建的用户对象
     */
    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
