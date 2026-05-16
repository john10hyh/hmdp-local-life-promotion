package com.hmdp.mq;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.VoucherOrderMessage;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Slf4j
@Component
public class VoucherOrderConsumer {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @KafkaListener(
            topics = "voucher-order-create",
            groupId = "voucher-order-service",
            autoStartup = "${spring.kafka.listener.auto-startup:true}"
    )
    @Transactional
    public void createVoucherOrder(String messageJson) {
        VoucherOrderMessage message = JSONUtil.toBean(messageJson, VoucherOrderMessage.class);

        Long count = voucherOrderService.query()
                .eq("user_id", message.getUserId())
                .eq("voucher_id", message.getVoucherId())
                .count();
        if (count > 0) {
            log.info("重复下单消息已忽略，userId={}, voucherId={}", message.getUserId(), message.getVoucherId());
            return;
        }

        boolean deducted = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", message.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!deducted) {
            throw new IllegalStateException("秒杀券库存不足，voucherId=" + message.getVoucherId());
        }

        VoucherOrder order = new VoucherOrder();
        order.setId(message.getOrderId());
        order.setUserId(message.getUserId());
        order.setVoucherId(message.getVoucherId());
        order.setStatus(1);
        voucherOrderService.save(order);
    }
}
