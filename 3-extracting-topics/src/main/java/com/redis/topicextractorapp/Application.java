package com.redis.topicextractorapp;

import com.redis.om.spring.annotations.EnableRedisEnhancedRepositories;
import com.redis.om.spring.client.RedisModulesClient;
import com.redis.om.spring.ops.pds.TopKOperations;
import com.redis.om.spring.ops.pds.TopKOperationsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.resps.StreamEntry;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@EnableRedisEnhancedRepositories
@SpringBootApplication
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled();
    }

    @Bean
    public TopKOperations<String> topKOperations(RedisModulesClient redisModulesClient) {
        return new TopKOperationsImpl<>(redisModulesClient);
    }

    @Bean
    public OllamaChatModel chatModel() {
        // Implement OllamaChatModel bean with appropriate configuration
        return null;
    }

    @Bean
    public CommandLineRunner runFilteringPipeline(
            RedisStreamService redisStreamService,
            StreamEventRepository streamEventRepository,
            TopicExtractionService topicExtractionService,
            TopKService topKService) {
        return args -> {
            String streamName = "filtered-events";
            String consumerGroup = "topic-extraction-group";

            redisStreamService.createConsumerGroup(streamName, consumerGroup);

            consumeStream(
                    streamName,
                    consumerGroup,
                    "topic-extractor-consumer-1",
                    streamEventRepository,
                    redisStreamService,
                    topKService,
                    topicExtractionService
            );
        };
    }

    private void consumeStream(
            String streamName,
            String consumerGroup,
            String consumer,
            StreamEventRepository streamEventRepository,
            RedisStreamService redisStreamService,
            TopKService topKService,
            TopicExtractionService topicExtractionService
    ) {
        while (!Thread.currentThread().isInterrupted()) {
            List<Map.Entry<String, List<StreamEntry>>> entries = redisStreamService.readFromStream(
                    streamName, consumerGroup, consumer, 5);

            List<StreamEvent> streamEvents = entries.stream().flatMap(entry ->
                            entry.getValue()
                                    .stream()
                                    .map(StreamEvent::fromStreamEntry)
            ).toList();

            streamEvents.forEach(event -> {
                logger.info("Filtered event: {}", event.getUri());
                List<String> topics = topicExtractionService.processTopics(event);

                if (!topics.isEmpty()) {
                    // Implement logic to save topics to their respective posts and to TopK in Redis
                }

                // Acknowledge the message
                redisStreamService.acknowledgeMessage(streamName, consumerGroup, event.getRedisStreamEntryId());
            });
        }
    }
}
