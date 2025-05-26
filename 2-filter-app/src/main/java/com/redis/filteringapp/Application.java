package com.redis.filteringapp;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.om.spring.annotations.EnableRedisEnhancedRepositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.util.Pair;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.resps.StreamEntry;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public CommandLineRunner runFilteringPipeline(
            RedisStreamService redisStreamService,
            ContentFilterService contentFilterService,
            StreamEventRepository streamEventRepository,
            FilteringExampleRepository filteringExampleRepository
    ) {
        return args -> {
            contentFilterService.loadReferences();

            String streamName = "jetstream";
            String consumerGroup = "filter-group";

            redisStreamService.createConsumerGroup(streamName, consumerGroup);

            consumeStream(
                    streamName,
                    consumerGroup,
                    "filter-consumer-1",
                    streamEventRepository,
                    redisStreamService,
                    contentFilterService
            );
        };
    }

    private void consumeStream(
            String streamName,
            String consumerGroup,
            String consumer,
            StreamEventRepository streamEventRepository,
            RedisStreamService redisStreamService,
            ContentFilterService contentFilterService
    ) {
        while (!Thread.currentThread().isInterrupted()) {
            List<Map.Entry<String, List<StreamEntry>>> entries = redisStreamService.readFromStream(
                    streamName, consumerGroup, consumer, 5);

            List<StreamEvent> events = entries.stream()
                    .flatMap(entry -> entry.getValue().stream())
                    .map(StreamEvent::fromStreamEntry)
                    .filter(this::filter)
                    .toList();

            List<Pair<StreamEvent, Boolean>> results = contentFilterService.isAiRelated(events);
            List<StreamEvent> toBeStored = results.stream().map(pair -> {
                StreamEvent event = pair.getFirst();
                boolean isRelated = pair.getSecond();

                // Acknowledge the message
                redisStreamService.acknowledgeMessage(streamName, consumerGroup, event.getRedisStreamEntryId());

                if (isRelated) {
                    logger.info("Filtered event: {}", event.getUri());

                    // Add to filtered stream
                    redisStreamService.addToStream("filtered-events", event.toMap());

                    // Return the event for being stored
                    return event;
                } else {
                    return null;
                }
            }).filter(Objects::nonNull).toList();

            // Save filtered events to the repository
            streamEventRepository.saveAll(toBeStored);
            logger.info("Processed {} events, stored {} filtered events",
                    events.size(), toBeStored.size());
        }
    }

    private boolean filter(StreamEvent event) {
        // Skip if text is empty or operation is delete
        return event.getText() != null && !event.getText().isBlank() && !"delete".equals(event.getOperation());
    }
}

