package com.solvemeup.smuworkerapi.worker.message;

import com.solvemeup.smuworkerapi.worker.config.RabbitConfig;
import com.solvemeup.smuworkerapi.worker.dto.RunResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunResultProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendResult(RunResultMessage message) {
        log.info("Sending run result - executionId: {}, caseIndex: {}, status: {}",
                message.executionId(), message.caseIndex(), message.status());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXAMPLE_EXCHANGE,
                    RabbitConfig.RUN_RESULT_ROUTING_KEY,
                    message
            );
            log.debug("Run result sent successfully - executionId: {}, caseIndex: {}",
                    message.executionId(), message.caseIndex());
        } catch (Exception e) {
            log.error("Failed to send run result - executionId: {}, caseIndex: {}",
                    message.executionId(), message.caseIndex(), e);
            throw e;
        }
    }
}
