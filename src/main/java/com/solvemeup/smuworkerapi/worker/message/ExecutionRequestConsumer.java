package com.solvemeup.smuworkerapi.worker.message;

import com.solvemeup.smuworkerapi.worker.config.RabbitConfig;
import com.solvemeup.smuworkerapi.worker.dto.ExecutionRequestMessage;
import com.solvemeup.smuworkerapi.worker.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionRequestConsumer {

    private final ExecutionService executionService;

    @RabbitListener(queues = RabbitConfig.EXECUTION_REQUEST_QUEUE)
    public void consume(ExecutionRequestMessage message) {
        log.info("Received execution request - executionId: {}, problemId: {}, caseCount: {}",
                message.executionId(), message.problemId(), message.testCases().size());

        try {
            executionService.execute(message);
        } catch (Exception e) {
            log.error("Fatal error processing execution request - executionId: {}", message.executionId(), e);
            throw e;
        }
    }
}
