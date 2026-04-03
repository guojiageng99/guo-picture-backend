package com.guo.guopicturebackend.outpainting;

import com.guo.guopicturebackend.config.OutpaintingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutPaintingDispatchService {

    private final OutpaintingProperties outpaintingProperties;
    private final OutPaintingTaskProcessor outPaintingTaskProcessor;
    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;

    private ExecutorService executor;

    @PostConstruct
    public void init() {
        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "outpaint-local");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    public void destroy() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void dispatchAfterCommit(Long taskId) {
        if (taskId == null) {
            return;
        }
        if (outpaintingProperties.isMqEnabled()) {
            RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
            if (rabbitTemplate != null) {
                try {
                    rabbitTemplate.convertAndSend(
                            OutPaintingRabbitConfig.EXCHANGE,
                            OutPaintingRabbitConfig.ROUTING_KEY,
                            String.valueOf(taskId));
                    return;
                } catch (Exception e) {
                    log.error("outpaint MQ send failed, fallback local taskId={}", taskId, e);
                }
            }
        }
        // 未开 MQ、无 Template、或发 MQ 失败时走本地线程池（成功发 MQ 则已在上方 return）
        executor.execute(() -> runProcess(taskId));
    }

    private void runProcess(Long taskId) {
        try {
            outPaintingTaskProcessor.processSubmit(taskId);
        } catch (Exception e) {
            log.error("outpaint processSubmit failed taskId={}", taskId, e);
        }
    }
}
