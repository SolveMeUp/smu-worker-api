package com.solvemeup.smuworkerapi.worker.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String JUDGE_EXCHANGE = "judge.exchange";
    public static final String JUDGE_REQUEST_ROUTING_KEY = "judge.request";
    public static final String JUDGE_RESULT_ROUTING_KEY = "judge.result";
    public static final String JUDGE_REQUEST_QUEUE = "judge.request.queue";

    public static final String EXAMPLE_EXCHANGE = "example.exchange";
    public static final String RUN_REQUEST_ROUTING_KEY = "example.run";
    public static final String RUN_RESULT_ROUTING_KEY = "example.result";
    public static final String RUN_REQUEST_QUEUE = "example.run.queue";

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
