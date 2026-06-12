package com.solvemeup.smuworkerapi.worker.message;

import com.solvemeup.smuworkerapi.worker.config.RabbitConfig;
import com.solvemeup.smuworkerapi.worker.dto.SubmissionResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionResultProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendResult(SubmissionResultMessage message) {
        log.info("Sending submission result - submissionId: {}, verdict: {}, time: {}ms, memory: {}KB",
                message.submissionId(), message.verdict(), message.timeUsedMillis(), message.memoryUsedKilobytes());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.SUBMISSION_EXCHANGE,
                    RabbitConfig.SUBMISSION_RESULT_ROUTING_KEY,
                    message
            );
            log.debug("Submission result sent successfully - submissionId: {}", message.submissionId());
        } catch (Exception e) {
            log.error("Failed to send submission result - submissionId: {}", message.submissionId(), e);
            throw e;
        }
    }
}
