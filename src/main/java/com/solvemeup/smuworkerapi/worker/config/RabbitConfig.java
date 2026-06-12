package com.solvemeup.smuworkerapi.worker.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String SUBMISSION_EXCHANGE = "submission.exchange";
    public static final String SUBMISSION_REQUEST_ROUTING_KEY = "submission.request";
    public static final String SUBMISSION_RESULT_ROUTING_KEY = "submission.result";
    public static final String SUBMISSION_REQUEST_QUEUE = "submission.request.queue";

    public static final String EXECUTION_EXCHANGE = "execution.exchange";
    public static final String EXECUTION_REQUEST_ROUTING_KEY = "execution.request";
    public static final String EXECUTION_RESULT_ROUTING_KEY = "execution.result";
    public static final String EXECUTION_REQUEST_QUEUE = "execution.request.queue";

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
