package com.hmdp.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic voucherOrderCreateTopic() {
        return new NewTopic("voucher-order-create", 1, (short) 1);
    }
}
