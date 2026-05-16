package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.VoucherOrderMessage;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mq.VoucherOrderConsumer;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false"
})
class VoucherOrderConsumerTest {

    @Resource
    private VoucherOrderConsumer voucherOrderConsumer;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private IVoucherOrderService voucherOrderService;

    @MockBean
    private KafkaAdmin kafkaAdmin;

    @Test
    @Transactional
    void createVoucherOrderShouldDeductMysqlStockAndCreatePendingOrder() {
        Long voucherId = 1301L;
        Long userId = 2301L;
        Long orderId = 3301L;
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucherId);
        seckillVoucher.setStock(12);
        seckillVoucher.setBeginTime(LocalDateTime.now().minusMinutes(1));
        seckillVoucher.setEndTime(LocalDateTime.now().plusHours(1));
        seckillVoucherService.save(seckillVoucher);

        VoucherOrderMessage message = new VoucherOrderMessage();
        message.setOrderId(orderId);
        message.setUserId(userId);
        message.setVoucherId(voucherId);
        message.setCreateTime(LocalDateTime.now());

        voucherOrderConsumer.createVoucherOrder(JSONUtil.toJsonStr(message));

        SeckillVoucher updatedVoucher = seckillVoucherService.getById(voucherId);
        VoucherOrder order = voucherOrderService.getById(orderId);
        assertThat(updatedVoucher.getStock()).isEqualTo(11);
        assertThat(order).isNotNull();
        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getVoucherId()).isEqualTo(voucherId);
        assertThat(order.getStatus()).isEqualTo(1);
    }

    @Test
    @Transactional
    void createVoucherOrderShouldIgnoreDuplicateUserVoucherMessage() {
        Long voucherId = 1302L;
        Long userId = 2302L;
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucherId);
        seckillVoucher.setStock(12);
        seckillVoucher.setBeginTime(LocalDateTime.now().minusMinutes(1));
        seckillVoucher.setEndTime(LocalDateTime.now().plusHours(1));
        seckillVoucherService.save(seckillVoucher);

        VoucherOrderMessage firstMessage = new VoucherOrderMessage();
        firstMessage.setOrderId(3302L);
        firstMessage.setUserId(userId);
        firstMessage.setVoucherId(voucherId);
        firstMessage.setCreateTime(LocalDateTime.now());

        VoucherOrderMessage duplicateMessage = new VoucherOrderMessage();
        duplicateMessage.setOrderId(3303L);
        duplicateMessage.setUserId(userId);
        duplicateMessage.setVoucherId(voucherId);
        duplicateMessage.setCreateTime(LocalDateTime.now());

        voucherOrderConsumer.createVoucherOrder(JSONUtil.toJsonStr(firstMessage));
        voucherOrderConsumer.createVoucherOrder(JSONUtil.toJsonStr(duplicateMessage));

        SeckillVoucher updatedVoucher = seckillVoucherService.getById(voucherId);
        Long orderCount = voucherOrderService.query()
                .eq("user_id", userId)
                .eq("voucher_id", voucherId)
                .count();

        assertThat(updatedVoucher.getStock()).isEqualTo(11);
        assertThat(orderCount).isEqualTo(1);
        assertThat(voucherOrderService.getById(3303L)).isNull();
    }

    @Test
    @Transactional
    void createVoucherOrderShouldFailWhenMysqlStockIsEmpty() {
        Long voucherId = 1303L;
        Long userId = 2303L;
        Long orderId = 3304L;
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucherId);
        seckillVoucher.setStock(0);
        seckillVoucher.setBeginTime(LocalDateTime.now().minusMinutes(1));
        seckillVoucher.setEndTime(LocalDateTime.now().plusHours(1));
        seckillVoucherService.save(seckillVoucher);

        VoucherOrderMessage message = new VoucherOrderMessage();
        message.setOrderId(orderId);
        message.setUserId(userId);
        message.setVoucherId(voucherId);
        message.setCreateTime(LocalDateTime.now());

        assertThatThrownBy(() -> voucherOrderConsumer.createVoucherOrder(JSONUtil.toJsonStr(message)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("库存不足");

        SeckillVoucher updatedVoucher = seckillVoucherService.getById(voucherId);
        assertThat(updatedVoucher.getStock()).isZero();
        assertThat(voucherOrderService.getById(orderId)).isNull();
    }
}
