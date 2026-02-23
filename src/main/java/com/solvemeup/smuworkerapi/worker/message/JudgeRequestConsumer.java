package com.solvemeup.smuworkerapi.worker.message;

import com.solvemeup.smuworkerapi.worker.config.RabbitConfig;
import com.solvemeup.smuworkerapi.worker.dto.SubmissionRequestMessage;
import com.solvemeup.smuworkerapi.worker.service.JudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeRequestConsumer {

    private final JudgeService judgeService;

    @RabbitListener(queues = RabbitConfig.JUDGE_REQUEST_QUEUE)
    public void consume(SubmissionRequestMessage message) {
        log.info("Received submission request - submissionResultId: {}, problemId: {}, submissionId: {}",
                message.submissionResultId(), message.problemId(), message.submissionId());

        try {
            judgeService.judge(message);
        } catch (Exception e) {
            log.error("Fatal error processing submission request - submissionResultId: {}", message.submissionResultId(), e);
            throw e;
        }
    }
}
