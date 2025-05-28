package com.redis.dataanalysisapp;

import com.redis.om.spring.annotations.EnableRedisEnhancedRepositories;
import com.redis.om.spring.client.RedisModulesClient;
import com.redis.om.spring.ops.pds.TopKOperations;
import com.redis.om.spring.ops.pds.TopKOperationsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;
import redis.clients.jedis.JedisPooled;

import java.time.Duration;
import java.util.List;

@EnableScheduling
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
    public TopKOperations<String> topKOperations(RedisModulesClient redisModulesClient) {
        return new TopKOperationsImpl<>(redisModulesClient);
    }

    @Bean
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder();
    }

    @Bean
    public CommandLineRunner runFilteringPipeline(
            SemanticRouterService semanticRouterService
    ) {
        return args -> {
            // Load references if not already loaded
            if (!semanticRouterService.areReferencesLoaded()) {
                List<String> trendingTopicsRoute = List.of(
                    "What are the most mentioned topics?",
                    "What's trending right now?",
                    "What’s hot in the network",
                    "Top topics?",
                    "What are the most discussed topics?",
                    "What are the most popular topics?",
                    "What are the most talked about topics?",
                    "What are the most mentioned topics in the AI community?"
                );
                semanticRouterService.loadReferences(trendingTopicsRoute, "trending_topics", 0.2);

                List<String> summarizationRoute = List.of(
                    "What are people saying about {topics}?",
                    "What’s the buzz around {topics}?",
                    "Any chatter about {topics}?",
                    "What are folks talking about regarding {topics}?",
                    "What’s being said about {topics} lately?",
                    "What have people been posting about {topics}?",
                    "What's trending in conversations about {topics}?",
                    "What’s the latest talk on {topics}?",
                    "Any recent posts about {topics}?",
                    "What's the sentiment around {topics}?",
                    "What are people saying about {topic1} and {topic2}?",
                    "What are folks talking about when it comes to {topic1}, {topic2}, or both?",
                    "What’s being said about {topic1}, {topic2}, and others?",
                    "Is there any discussion around {topic1} and {topic2}?",
                    "How are people reacting to both {topic1} and {topic2}?",
                    "What’s the conversation like around {topic1}, {topic2}, or related topics?",
                    "Are {topic1} and {topic2} being discussed together?",
                    "Any posts comparing {topic1} and {topic2}?",
                    "What's trending when it comes to {topic1} and {topic2}?",
                    "What are people saying about the relationship between {topic1} and {topic2}?",
                    "What’s the latest discussion on {topic1} and {topic2}?"
                );
                semanticRouterService.loadReferences(summarizationRoute, "summarization", 0.55);
            }
        };
    }

    @Bean
    public CommandLineRunner createBloomFilter(
            BloomFilterService bloomFilterService
    ) {
        return args -> {
            bloomFilterService.createBloomFilter("processed-posts-bf");
        };
    }
}
