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
public class JudgeResultProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendResult(SubmissionResultMessage message) {
        log.info("Sending submission result - submissionResultId: {}, result: {}, time: {}ms, memory: {}KB",
                message.submissionResultId(), message.result(), message.timeUsedMillis(), message.memoryUsedKilobytes());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.JUDGE_EXCHANGE,
                    RabbitConfig.JUDGE_RESULT_ROUTING_KEY,
                    message
            );
            log.debug("Submission result sent successfully - submissionResultId: {}", message.submissionResultId());
        } catch (Exception e) {
            log.error("Failed to send submission result - submissionResultId: {}", message.submissionResultId(), e);
            throw e;
        }
    }
}
