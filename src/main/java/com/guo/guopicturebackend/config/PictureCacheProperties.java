package com.guo.guopicturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 多级缓存与热点图片相关配置（防击穿/穿透/雪崩 + 热点自动本地缓存）
 */
@Data
@ConfigurationProperties(prefix = "picture.cache")
public class PictureCacheProperties {

    /** 公共列表 Redis 基础 TTL（秒），实际会加随机抖动防雪崩 */
    private int listRedisTtlBaseSeconds = 300;

    /** 公共列表 Redis TTL 随机上界（秒），最终 TTL ∈ [base, base+jitter) */
    private int listRedisTtlJitterSeconds = 300;

    /** 空列表防穿透：短 TTL 基础值（秒） */
    private int listEmptyTtlBaseSeconds = 60;

    private int listEmptyTtlJitterSeconds = 40;

    /** 列表重建分布式锁持有时间（秒），基于 Redis SETNX */
    private int listRebuildLockSeconds = 10;

    /** 锁竞争时自旋等待次数 */
    private int listLockSpinMaxAttempts = 30;

    /** 单次自旋休眠毫秒（基础上再加少量随机） */
    private int listLockSpinSleepMs = 45;

    /** 图片实体 Redis TTL 基础（秒） */
    private int detailEntityTtlBaseSeconds = 180;

    private int detailEntityTtlJitterSeconds = 120;

    /** 不存在图片：空值防穿透 TTL */
    private int detailNullTtlBaseSeconds = 60;

    private int detailNullTtlJitterSeconds = 30;

    /** 窗口内访问次数达到阈值则视为热点，写入本地延长缓存 */
    private int hotAccessThreshold = 40;

    /** 热点统计窗口（秒） */
    private int hotWindowSeconds = 120;

    /** 热点图片本地缓存写入后过期时间（分钟） */
    private int hotLocalExpireMinutes = 15;

    /** 公共列表本地缓存（Caffeine）最大条数 */
    private long listLocalMaxSize = 2000;

    /** 热点实体本地最大条数 */
    private long hotEntityLocalMaxSize = 500;
}
