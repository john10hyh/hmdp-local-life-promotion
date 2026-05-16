package com.hmdp.service.impl;

import com.hmdp.utils.RedisConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaAdmin;

import javax.annotation.Resource;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false"
})
class SeckillLuaScriptTest {

    private static final String SECKILL_ORDER_KEY = "seckill:order:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private KafkaAdmin kafkaAdmin;

    @Test
    void seckillScriptShouldReturnStockNotEnoughWhenStockIsZero() {
        Long voucherId = 1001L;
        Long userId = 2001L;
        String stockKey = RedisConstants.SECKILL_STOCK_KEY + voucherId;
        String orderKey = SECKILL_ORDER_KEY + voucherId;

        DefaultRedisScript<Long> seckillScript = new DefaultRedisScript<>();
        seckillScript.setLocation(new ClassPathResource("lua/seckill.lua"));
        seckillScript.setResultType(Long.class);

        try {
            stringRedisTemplate.opsForValue().set(stockKey, "0");
            stringRedisTemplate.delete(orderKey);

            Long result = stringRedisTemplate.execute(
                    seckillScript,
                    Collections.emptyList(),
                    voucherId.toString(),
                    userId.toString()
            );

            assertThat(result).isEqualTo(1L);
        } finally {
            stringRedisTemplate.delete(stockKey);
            stringRedisTemplate.delete(orderKey);
        }
    }

    @Test
    void seckillScriptShouldReturnDuplicateOrderWhenUserAlreadyOrdered() {
        Long voucherId = 1002L;
        Long userId = 2002L;
        String stockKey = RedisConstants.SECKILL_STOCK_KEY + voucherId;
        String orderKey = SECKILL_ORDER_KEY + voucherId;

        DefaultRedisScript<Long> seckillScript = new DefaultRedisScript<>();
        seckillScript.setLocation(new ClassPathResource("lua/seckill.lua"));
        seckillScript.setResultType(Long.class);

        try {
            stringRedisTemplate.opsForValue().set(stockKey, "12");
            stringRedisTemplate.opsForSet().add(orderKey, userId.toString());

            Long result = stringRedisTemplate.execute(
                    seckillScript,
                    Collections.emptyList(),
                    voucherId.toString(),
                    userId.toString()
            );

            assertThat(result).isEqualTo(2L);
        } finally {
            stringRedisTemplate.delete(stockKey);
            stringRedisTemplate.delete(orderKey);
        }
    }

    @Test
    void seckillScriptShouldDeductStockAndRecordUserWhenQualified() {
        Long voucherId = 1003L;
        Long userId = 2003L;
        String stockKey = RedisConstants.SECKILL_STOCK_KEY + voucherId;
        String orderKey = SECKILL_ORDER_KEY + voucherId;

        DefaultRedisScript<Long> seckillScript = new DefaultRedisScript<>();
        seckillScript.setLocation(new ClassPathResource("lua/seckill.lua"));
        seckillScript.setResultType(Long.class);

        try {
            stringRedisTemplate.opsForValue().set(stockKey, "12");
            stringRedisTemplate.delete(orderKey);

            Long result = stringRedisTemplate.execute(
                    seckillScript,
                    Collections.emptyList(),
                    voucherId.toString(),
                    userId.toString()
            );

            assertThat(result).isEqualTo(0L);
            assertThat(stringRedisTemplate.opsForValue().get(stockKey)).isEqualTo("11");
            assertThat(stringRedisTemplate.opsForSet().isMember(orderKey, userId.toString())).isTrue();
        } finally {
            stringRedisTemplate.delete(stockKey);
            stringRedisTemplate.delete(orderKey);
        }
    }
}
