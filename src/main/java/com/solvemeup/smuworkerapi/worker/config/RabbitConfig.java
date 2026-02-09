package com.solvemeup.smuworkerapi.worker.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String JUDGE_EXCHANGE  = "judge.exchange";
    public static final String REQUEST_ROUTING_KEY = "judge.request";
    public static final String RESULT_ROUTING_KEY = "judge.result";

    public static final String REQUEST_QUEUE = "judge.request.queue";

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter(){
        return new JacksonJsonMessageConverter();
    }
}
