package com.guo.guopicturebackend.outpainting;

import com.guo.guopicturebackend.config.OutpaintingProperties;
import com.guo.guopicturebackend.exception.BusinessException;
import com.guo.guopicturebackend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OutPaintingRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;
    private final OutpaintingProperties outpaintingProperties;

    public void checkOrThrow(Long userId) {
        if (userId == null) {
            return;
        }
        String key = "outpaint:rl:" + userId;
        Long c = stringRedisTemplate.opsForValue().increment(key);
        if (c != null && c == 1L) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(1));
        }
        int limit = Math.max(1, outpaintingProperties.getRateLimitPerMinute());
        if (c != null && c > limit) {
            throw new BusinessException(ErrorCode.OUTPAINT_RATE_LIMIT);
        }
    }
}
