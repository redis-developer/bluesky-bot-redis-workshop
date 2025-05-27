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
                .apiKey(System.getenv("OPEN_AI_KEY"))
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
        };
    }

    @Bean
    public CommandLineRunner createBloomFilter(
            BloomFilterService bloomFilterService
    ) {
        return args -> {
            //bloomFilterService.createBloomFilter("processed-posts-bf");
        };
    }
}
