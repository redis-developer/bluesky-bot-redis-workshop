package com.redis.consumerapp;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.JedisPooled;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled();
    }

    @Bean
    public CommandLineRunner run(
            JetstreamClient client,
            RedisStreamService redisStreamService) {
        return args -> {
            client.setMessageConsumer(message -> {
                BlueskyEvent blueskyEvent = BlueskyEvent.fromJson(message);
                redisStreamService.addToStream("jetstream", blueskyEvent.toMap());
            });
            client.start();
        };
    }
}
