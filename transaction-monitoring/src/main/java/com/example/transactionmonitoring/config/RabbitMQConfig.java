package com.example.transactionmonitoring.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the RabbitMQ exchange, queue and binding used to decouple
 * transaction persistence from asynchronous rule evaluation.
 */
@Configuration
public class RabbitMQConfig {

    public static final String TRANSACTION_EXCHANGE = "transaction.exchange";
    public static final String TRANSACTION_CREATED_QUEUE = "transaction.created.queue";
    public static final String TRANSACTION_CREATED_ROUTING_KEY = "transaction.created";

    @Bean
    public DirectExchange transactionExchange() {
        return new DirectExchange(TRANSACTION_EXCHANGE);
    }

    @Bean
    public Queue transactionCreatedQueue() {
        return new Queue(TRANSACTION_CREATED_QUEUE, true);
    }

    @Bean
    public Binding transactionCreatedBinding(
            Queue transactionCreatedQueue,
            DirectExchange transactionExchange
    ) {
        return BindingBuilder.bind(transactionCreatedQueue)
                .to(transactionExchange)
                .with(TRANSACTION_CREATED_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
