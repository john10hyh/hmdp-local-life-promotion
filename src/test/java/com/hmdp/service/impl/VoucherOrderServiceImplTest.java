package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false"
})
class VoucherOrderServiceImplTest {

    private static final String SECKILL_ORDER_KEY = "seckill:order:";
    private static final String VOUCHER_ORDER_CREATE_TOPIC = "voucher-order-create";

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @MockBean(name = "kafkaTemplate")
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private KafkaAdmin kafkaAdmin;

    @Test
    void seckillVoucherShouldFailWhenRedisStockNotEnough() {
        Long voucherId = 1101L;
        Long userId = 2101L;
        String stockKey = RedisConstants.SECKILL_STOCK_KEY + voucherId;
        String orderKey = SECKILL_ORDER_KEY + voucherId;
        UserDTO user = new UserDTO();
        user.setId(userId);

        try {
            UserHolder.saveUser(user);
            stringRedisTemplate.opsForValue().set(stockKey, "0");
            stringRedisTemplate.delete(orderKey);

            Result result = voucherOrderService.seckillVoucher(voucherId);

            assertThat(result.getSuccess()).isFalse();
            assertThat(result.getErrorMsg()).isEqualTo("库存不足");
        } finally {
            UserHolder.removeUser();
            stringRedisTemplate.delete(stockKey);
            stringRedisTemplate.delete(orderKey);
        }
    }

    @Test
    void seckillVoucherShouldFailWhenUserAlreadyOrdered() {
        Long voucherId = 1102L;
        Long userId = 2102L;
        String stockKey = RedisConstants.SECKILL_STOCK_KEY + voucherId;
        String orderKey = SECKILL_ORDER_KEY + voucherId;
        UserDTO user = new UserDTO();
        user.setId(userId);

        try {
            UserHolder.saveUser(user);
            stringRedisTemplate.opsForValue().set(stockKey, "12");
            stringRedisTemplate.opsForSet().add(orderKey, userId.toString());

            Result result = voucherOrderService.seckillVoucher(voucherId);

            assertThat(result.getSuccess()).isFalse();
            assertThat(result.getErrorMsg()).isEqualTo("不能重复下单");
        } finally {
            UserHolder.removeUser();
            stringRedisTemplate.delete(stockKey);
            stringRedisTemplate.delete(orderKey);
        }
    }

    @Test
    void seckillVoucherShouldReturnOrderIdWhenQualified() {
        Long voucherId = 1103L;
        Long userId = 2103L;
        String stockKey = RedisConstants.SECKILL_STOCK_KEY + voucherId;
        String orderKey = SECKILL_ORDER_KEY + voucherId;
        UserDTO user = new UserDTO();
        user.setId(userId);

        try {
            UserHolder.saveUser(user);
            stringRedisTemplate.opsForValue().set(stockKey, "12");
            stringRedisTemplate.delete(orderKey);

            Result result = voucherOrderService.seckillVoucher(voucherId);

            assertThat(result.getSuccess()).isTrue();
            assertThat(result.getData()).isInstanceOf(Long.class);
            assertThat((Long) result.getData()).isPositive();
            assertThat(stringRedisTemplate.opsForValue().get(stockKey)).isEqualTo("11");
            assertThat(stringRedisTemplate.opsForSet().isMember(orderKey, userId.toString())).isTrue();
            verify(kafkaTemplate).send(eq(VOUCHER_ORDER_CREATE_TOPIC), anyString(), anyString());
        } finally {
            UserHolder.removeUser();
            stringRedisTemplate.delete(stockKey);
            stringRedisTemplate.delete(orderKey);
        }
    }
}
