package com.julien.saevecke.learner.config.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String SUL_INPUT_ROUTING_KEY = "sul.input";
    public static final String SUL_OUTPUT_ROUTING_KEY = "sul.output";
    public static final String SUL_DIRECT_EXCHANGE = "sul_dx";
    public static final String SUL_INPUT_QUEUE = "sul_input_q";
    public static final String SUL_OUTPUT_QUEUE = "sul_output_q";

    @Bean
    public Queue sulInputQueue() {
        return new Queue(SUL_INPUT_QUEUE);
    }
    @Bean
    public Queue sulOutputQueue() { return new Queue(SUL_OUTPUT_QUEUE); }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(SUL_DIRECT_EXCHANGE);
    }

    @Bean
    public Binding bindingSULQueue(DirectExchange exchange) {
        return BindingBuilder.bind(sulInputQueue()).to(exchange).with(SUL_INPUT_ROUTING_KEY);
    }

    @Bean
    public Binding bindingMOQueue(DirectExchange exchange) {
        return BindingBuilder.bind(sulOutputQueue()).to(exchange).with(SUL_OUTPUT_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate template(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
}
