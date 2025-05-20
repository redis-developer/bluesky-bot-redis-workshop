package com.redis.filteringapp;
import com.redis.om.spring.annotations.EnableRedisEnhancedRepositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.resps.StreamEntry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
            BloomFilterService bloomFilterService,
            ContentFilterService contentFilterService,
            StreamEventRepository streamEventRepository
    ) {
        return args -> {
            String streamName = "jetstream";
            String consumerGroup = "filter-group";
            String bloomFilterName = "filter-dedup-bf";

            redisStreamService.createConsumerGroup(streamName, consumerGroup);
            bloomFilterService.createBloomFilter(bloomFilterName);

            int numConsumers = 4;

            ExecutorService executorService = Executors.newFixedThreadPool(numConsumers);
            for (int i = 0; i < numConsumers; i++) {
                final String consumerName = "consumer-" + i;
                executorService.submit(() -> consumeStream(
                        streamName,
                        consumerGroup,
                        consumerName,
                        bloomFilterName,
                        streamEventRepository,
                        redisStreamService,
                        bloomFilterService,
                        contentFilterService
                ));
            }


            // Add shutdown hook to close resources
            Runtime.getRuntime().addShutdownHook(new Thread(contentFilterService::close));        };
    }


    private void consumeStream(
            String streamName,
            String consumerGroup,
            String consumer,
            String bloomFilterName,
            StreamEventRepository streamEventRepository,
            RedisStreamService redisStreamService,
            BloomFilterService bloomFilterService,
            ContentFilterService contentFilterService
    ) {
        while (!Thread.currentThread().isInterrupted()) {
            List<Map.Entry<String, List<StreamEntry>>> entries = redisStreamService.readFromStream(
                    streamName, consumerGroup, consumer, 5);

            for (Map.Entry<String, List<StreamEntry>> streamEntries : entries) {
                for (StreamEntry entry : streamEntries.getValue()) {
                    StreamEvent event = StreamEvent.fromStreamEntry(entry);

                    // Process the event through our pipeline
                    if (processEvent(
                            event,
                            bloomFilterName,
                            bloomFilterService,
                            contentFilterService
                    )) {
                        logger.info("Filtered event: {}", event.getUri());
                        // Store the filtered event
                        streamEventRepository.save(event);
                        // Add to filtered stream
                        redisStreamService.addToStream("filtered-events", event.toMap());
                    }

                    // Acknowledge the message
                    redisStreamService.acknowledgeMessage(streamName, consumerGroup, entry);
                    // Add to bloom filter for deduplication

                    bloomFilterService.addToBloomFilter(bloomFilterName, event.getUri());
                }
            }
        }
    }

    private boolean processEvent(
            StreamEvent event,
            String bloomFilterName,
            BloomFilterService bloomFilterService,
            ContentFilterService contentFilterService
    ) {
        // Skip if already processed (deduplication)
        if (bloomFilterService.isInBloomFilter(bloomFilterName, event.getUri())) {
            logger.info("Event already processed: {}", event.getUri());
            return false;
        }

        // Skip if text is empty or operation is delete
        if (event.getText() == null || event.getText().isBlank() || "delete".equals(event.getOperation())) {
            return false;
        }

        // Filter based on content
        if (!contentFilterService.isPoliticsRelated(event.getText())) {
            return false;
        }

        logger.info("Storing event: {}", event.getUri());
        return true;
    }
}

