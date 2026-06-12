package com.solvemeup.smuworkerapi.worker.message;

import com.solvemeup.smuworkerapi.worker.config.RabbitConfig;
import com.solvemeup.smuworkerapi.worker.dto.ExecutionResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionResultProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendResult(ExecutionResultMessage message) {
        log.info("Sending execution result - executionId: {}, caseIndex: {}, verdict: {}",
                message.executionId(), message.caseIndex(), message.verdict());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXECUTION_EXCHANGE,
                    RabbitConfig.EXECUTION_RESULT_ROUTING_KEY,
                    message
            );
            log.debug("Execution result sent successfully - executionId: {}, caseIndex: {}",
                    message.executionId(), message.caseIndex());
        } catch (Exception e) {
            log.error("Failed to send execution result - executionId: {}, caseIndex: {}",
                    message.executionId(), message.caseIndex(), e);
            throw e;
        }
    }
}
