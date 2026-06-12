package com.solvemeup.smuworkerapi.worker.message;

import com.solvemeup.smuworkerapi.worker.config.RabbitConfig;
import com.solvemeup.smuworkerapi.worker.dto.SubmissionRequestMessage;
import com.solvemeup.smuworkerapi.worker.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionRequestConsumer {

    private final SubmissionService submissionService;

    @RabbitListener(queues = RabbitConfig.SUBMISSION_REQUEST_QUEUE)
    public void consume(SubmissionRequestMessage message) {
        log.info("Received submission request - submissionId: {}, problemId: {}",
                message.submissionId(), message.problemId());

        try {
            submissionService.judge(message);
        } catch (Exception e) {
            log.error("Fatal error processing submission request - submissionId: {}", message.submissionId(), e);
            throw e;
        }
    }
}
