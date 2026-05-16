package com.hmdp.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConstantsTest {

    @Test
    void loginCodeTtlShouldBeTenMinutesForManualTesting() {
        assertThat(RedisConstants.LOGIN_CODE_TTL).isEqualTo(10L);
    }
}
