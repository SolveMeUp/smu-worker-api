package com.solvemeup.smuworkerapi.worker.message;

import com.solvemeup.smuworkerapi.worker.config.RabbitConfig;
import com.solvemeup.smuworkerapi.worker.dto.JudgeResponseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeResultProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendResult(JudgeResponseMessage message) {
        log.info("Sending judge result - judgeId: {}, result: {}, time: {}ms, memory: {}MB",
                message.judgeId(), message.result(), message.timeUsedMillis(), message.memoryUsedMegabytes());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.JUDGE_EXCHANGE,
                    RabbitConfig.RESULT_ROUTING_KEY,
                    message
            );
            log.debug("Judge result sent successfully - judgeId: {}", message.judgeId());
        } catch (Exception e) {
            log.error("Failed to send judge result - judgeId: {}", message.judgeId(), e);
            throw e;
        }
    }
}