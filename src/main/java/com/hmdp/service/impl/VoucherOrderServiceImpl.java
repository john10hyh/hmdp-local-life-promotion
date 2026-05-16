package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.dto.VoucherOrderMessage;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import cn.hutool.json.JSONUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    private static final String VOUCHER_ORDER_CREATE_TOPIC = "voucher-order-create";

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public Result seckillVoucher(Long voucherId) {
        UserDTO user = UserHolder.getUser();
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                user.getId().toString()
        );

        if (result != null && result == 1) {
            return Result.fail("库存不足");
        }
        if (result != null && result == 2) {
            return Result.fail("不能重复下单");
        }
        long orderId = redisIdWorker.nextId("order");
        VoucherOrderMessage message = new VoucherOrderMessage();
        message.setOrderId(orderId);
        message.setUserId(user.getId());
        message.setVoucherId(voucherId);
        message.setCreateTime(LocalDateTime.now());
        kafkaTemplate.send(VOUCHER_ORDER_CREATE_TOPIC, orderId + "", JSONUtil.toJsonStr(message));
        return Result.ok(orderId);
    }
}
