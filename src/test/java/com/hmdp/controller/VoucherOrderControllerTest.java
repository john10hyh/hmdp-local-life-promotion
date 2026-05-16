package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.IVoucherOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaAdmin;

import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false"
})
class VoucherOrderControllerTest {

    @Resource
    private VoucherOrderController voucherOrderController;

    @MockBean
    private IVoucherOrderService voucherOrderService;

    @MockBean
    private KafkaAdmin kafkaAdmin;

    @Test
    void seckillVoucherShouldDelegateToService() {
        Long voucherId = 1201L;
        when(voucherOrderService.seckillVoucher(voucherId)).thenReturn(Result.ok(9001L));

        Result result = voucherOrderController.seckillVoucher(voucherId);

        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(9001L);
        verify(voucherOrderService).seckillVoucher(voucherId);
    }
}
