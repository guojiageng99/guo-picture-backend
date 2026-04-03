package com.guo.guopicturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 扩图：额度、限流、超时对账、MQ 开关
 */
@Data
@ConfigurationProperties(prefix = "outpainting")
public class OutpaintingProperties {

    /**
     * 是否通过 RabbitMQ 投递提交任务；false 时在事务提交后用线程池异步执行
     */
    private boolean mqEnabled = true;

    /** 每用户每分钟最多提交次数 */
    private int rateLimitPerMinute = 3;

    /** 本地判定「长时间无终态」的分钟数（进入对账） */
    private int localTimeoutMinutes = 10;

    /** 进入 RECONCILING 后最多轮询云端次数（约等于分钟数，配合调度周期） */
    private int reconcileMaxAttempts = 6;

    /** 标准模式单次消耗次数 */
    private int quotaStandardCost = 1;

    /** 高清（best_quality=true）单次消耗次数 */
    private int quotaHdCost = 2;

    /** 新注册用户默认扩图次数 */
    private int newUserQuota = 10;
}
