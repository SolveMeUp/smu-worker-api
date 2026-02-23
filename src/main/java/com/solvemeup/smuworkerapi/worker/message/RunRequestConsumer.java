package com.solvemeup.smuworkerapi.worker.message;

import com.solvemeup.smuworkerapi.worker.config.RabbitConfig;
import com.solvemeup.smuworkerapi.worker.dto.RunRequestMessage;
import com.solvemeup.smuworkerapi.worker.service.RunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunRequestConsumer {

    private final RunService runService;

    @RabbitListener(queues = RabbitConfig.RUN_REQUEST_QUEUE)
    public void consume(RunRequestMessage message) {
        log.info("Received run request - runId: {}, caseCount: {}",
                message.runId(), message.testCases().size());

        try {
            runService.judge(message);
        } catch (Exception e) {
            log.error("Fatal error processing run request - runId: {}", message.runId(), e);
            throw e;
        }
    }
}
