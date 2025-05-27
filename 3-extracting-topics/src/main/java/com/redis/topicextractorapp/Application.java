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
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.resps.StreamEntry;

import java.time.Duration;
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
    public OpenAiChatModel chatModel() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout((int) Duration.ofSeconds(60).toMillis());

        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .restClientBuilder(RestClient.builder().requestFactory(factory))
            .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model("gpt-4o-mini")
            .build();

        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(options)
            .build();
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
                    Map<String, Long> counts = topics.stream().collect(Collectors.toMap(
                        topic -> topic,
                        topic -> 1L, // Initialize count to 1 for each topic
                        Long::sum // In case of duplicates (very unlikely), sum the counts
                    ));

                    // Create TopK
                    String topKKeySpace = "topics-topk:";
                    String topKKey = topKKeySpace + LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
                    topKService.create(topKKey);
                    topKService.incrBy(topKKey, counts);

                    event.setTopics(topics);
                    streamEventRepository.updateField(event, StreamEvent$.TOPICS, topics);
                }

                // Acknowledge the message
                redisStreamService.acknowledgeMessage(streamName, consumerGroup, event.getRedisStreamEntryId());
            });
        }
    }
}
