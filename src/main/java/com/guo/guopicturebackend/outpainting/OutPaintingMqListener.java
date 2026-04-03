package com.guo.guopicturebackend.outpainting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "outpainting.mq-enabled", havingValue = "true")
public class OutPaintingMqListener {

    private final OutPaintingTaskProcessor outPaintingTaskProcessor;

    @RabbitListener(queues = OutPaintingRabbitConfig.QUEUE)
    public void onMessage(String body) {
        if (body == null || body.isEmpty()) {
            return;
        }
        long taskId;
        try {
            taskId = Long.parseLong(body.trim());
        } catch (NumberFormatException e) {
            log.warn("outpaint MQ bad body: {}", body);
            return;
        }
        try {
            outPaintingTaskProcessor.processSubmit(taskId);
        } catch (Exception e) {
            log.error("outpaint MQ consume failed taskId={}", taskId, e);
        }
    }
}
