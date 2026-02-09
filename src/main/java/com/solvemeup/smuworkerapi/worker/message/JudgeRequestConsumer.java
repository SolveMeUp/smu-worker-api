package com.solvemeup.smuworkerapi.worker.message;

import com.solvemeup.smuworkerapi.worker.config.RabbitConfig;
import com.solvemeup.smuworkerapi.worker.dto.JudgeRequestMessage;
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

    @RabbitListener(queues = RabbitConfig.REQUEST_QUEUE)
    public void consume(JudgeRequestMessage message) {
        log.info("Received judge request - judgeId: {}, problemId: {}, submissionId: {}",
                message.judgeId(), message.problemId(), message.submissionId());

        try {
            judgeService.judge(message);
        } catch (Exception e) {
            log.error("Fatal error processing judge request - judgeId: {}", message.judgeId(), e);
            throw e;
        }
    }
}