package com.example.restaurant.service;

import com.example.restaurant.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {
    private final MutableClock clock = new MutableClock();
    private final LoginRateLimiter limiter = new LoginRateLimiter(
            2,
            Duration.ofMinutes(10),
            Duration.ofMinutes(5),
            clock);

    @Test
    void locksKeyAfterMaxFailures() {
        limiter.recordFailure("127.0.0.1:13800000000");
        assertThatCode(() -> limiter.assertAllowed("127.0.0.1:13800000000")).doesNotThrowAnyException();

        limiter.recordFailure("127.0.0.1:13800000000");

        assertThatThrownBy(() -> limiter.assertAllowed("127.0.0.1:13800000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("登录失败次数过多，请稍后再试");
    }

    @Test
    void clearRemovesFailureStateAfterSuccessfulLogin() {
        limiter.recordFailure("127.0.0.1:13800000000");
        limiter.clear("127.0.0.1:13800000000");

        assertThatCode(() -> limiter.assertAllowed("127.0.0.1:13800000000")).doesNotThrowAnyException();
    }

    @Test
    void unlocksAfterLockDurationExpires() {
        limiter.recordFailure("127.0.0.1:13800000000");
        limiter.recordFailure("127.0.0.1:13800000000");

        clock.advance(Duration.ofMinutes(6));

        assertThatCode(() -> limiter.assertAllowed("127.0.0.1:13800000000")).doesNotThrowAnyException();
    }

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-06-06T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
