package com.hmdp.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaAdmin;

import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false"
})
class KafkaTopicConfigTest {

    @Resource(name = "voucherOrderCreateTopic")
    private NewTopic voucherOrderCreateTopic;

    @MockBean
    private KafkaAdmin kafkaAdmin;

    @Test
    void voucherOrderCreateTopicShouldBeDeclared() {
        assertThat(voucherOrderCreateTopic.name()).isEqualTo("voucher-order-create");
        assertThat(voucherOrderCreateTopic.numPartitions()).isEqualTo(1);
        assertThat(voucherOrderCreateTopic.replicationFactor()).isEqualTo((short) 1);
    }
}
