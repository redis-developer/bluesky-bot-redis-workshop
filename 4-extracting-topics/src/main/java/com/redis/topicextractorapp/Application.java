package com.redis.topicextractorapp;

import com.redis.om.spring.annotations.EnableRedisEnhancedRepositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.resps.StreamEntry;

import java.time.LocalDateTime;
import java.util.HashMap;
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
    public OllamaChatModel chatModel() {
        OllamaApi ollamaApi = new OllamaApi("http://localhost:11434");

        OllamaOptions ollamaOptions = OllamaOptions.builder()
                .model("deepseek-coder-v2")
                .build();

        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(ollamaOptions)
                .build();
    }

    @Bean
    public CommandLineRunner runFilteringPipeline(
            RedisStreamService redisStreamService,
            BloomFilterService bloomFilterService,
            StreamEventRepository streamEventRepository,
            TopicExtractionService topicExtractionService,
            CountMinSketchService countMinSketchService) {
        return args -> {
            String streamName = "filtered-events";
            String consumerGroup = "topic-extraction-group";
            String bloomFilterName = "topic-extraction-dedup-bf";

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
                        countMinSketchService,
                        topicExtractionService
                ));
            }
        };
    }


    private void consumeStream(
            String streamName,
            String consumerGroup,
            String consumer,
            String bloomFilterName,
            StreamEventRepository streamEventRepository,
            RedisStreamService redisStreamService,
            BloomFilterService bloomFilterService,
            CountMinSketchService countMinSketchService,
            TopicExtractionService topicExtractionService
    ) {
        while (!Thread.currentThread().isInterrupted()) {
            List<Map.Entry<String, List<StreamEntry>>> entries = redisStreamService.readFromStream(
                    streamName, consumerGroup, consumer, 5);

            String cmsKeySpace = "topics-cms:";
            String cmsKey = cmsKeySpace + LocalDateTime.now().withMinute(0).withNano(0);
            countMinSketchService.create(cmsKey);

            for (Map.Entry<String, List<StreamEntry>> streamEntries : entries) {
                for (StreamEntry entry : streamEntries.getValue()) {
                    StreamEvent event = StreamEvent.fromStreamEntry(entry);

                    // Process the event through our pipeline
                    if (processEvent(
                            event,
                            bloomFilterName,
                            bloomFilterService
                    )) {
                        logger.info("Filtered event: {}", event.getUri());
                        List<String> topics = topicExtractionService.processTopics(event);

                        Map<String, Long> counts = new HashMap<>();
                        for (String topic : topics) {
                            counts.put(topic, 1L);
                        }
                        countMinSketchService.incrBy(cmsKey, counts);

                        event.setTopics(topics);
                        streamEventRepository.updateField(event, StreamEvent$.TOPICS, topics);
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
            BloomFilterService bloomFilterService
    ) {
        // Skip if already processed (deduplication)
        if (bloomFilterService.isInBloomFilter(bloomFilterName, event.getUri())) {
            logger.info("Event already processed: {}", event.getUri());
            return false;
        }

        logger.info("Extracting topics for event: {}", event.getUri());
        return true;
    }
}
