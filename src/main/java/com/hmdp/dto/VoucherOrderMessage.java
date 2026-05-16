package com.hmdp.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VoucherOrderMessage {
    private Long orderId;
    private Long userId;
    private Long voucherId;
    private LocalDateTime createTime;
}
