package com.hmdp.service.impl;

import com.hmdp.entity.Voucher;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false"
})
class VoucherServiceImplTest {

    @Resource
    private IVoucherService voucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private KafkaAdmin kafkaAdmin;

    @Test
    @Transactional
    void addSeckillVoucherShouldCacheStockInRedis() {
        Voucher voucher = new Voucher();
        voucher.setShopId(1L);
        voucher.setTitle("TDD 秒杀券");
        voucher.setSubTitle("新增秒杀券同步 Redis 库存");
        voucher.setRules("测试券");
        voucher.setPayValue(1000L);
        voucher.setActualValue(2000L);
        voucher.setType(1);
        voucher.setStatus(1);
        voucher.setStock(12);
        voucher.setBeginTime(LocalDateTime.now().minusMinutes(1));
        voucher.setEndTime(LocalDateTime.now().plusHours(1));

        try {
            voucherService.addSeckillVoucher(voucher);

            String redisStock = stringRedisTemplate.opsForValue()
                    .get(RedisConstants.SECKILL_STOCK_KEY + voucher.getId());

            assertThat(redisStock).isEqualTo("12");
        } finally {
            if (voucher.getId() != null) {
                stringRedisTemplate.delete(RedisConstants.SECKILL_STOCK_KEY + voucher.getId());
            }
        }
    }
}
