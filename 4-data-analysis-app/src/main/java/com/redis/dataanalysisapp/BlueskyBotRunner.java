package com.redis.dataanalysisapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class BlueskyBotRunner {

    @Value("${bluesky.did}")
    private String did;

    private static final Logger logger = LoggerFactory.getLogger(BlueskyBotRunner.class);
    private final BlueskyAuthService authService;
    private final PostSearcherService postSearcher;
    private final PostCreatorService postCreator;
    private final SemanticRouterService semanticRouterService;
    private final TrendingTopicsAnalyzer trendingTopicsAnalyzer;
    private final PostSummarizer postSummarizer;
    private final OpenAiChatModel openAiChatModel;
    private final SemanticCacheService semanticCacheService;
    private final BloomFilterService bloomFilterService;

    public BlueskyBotRunner(
            BlueskyAuthService authService,
            PostSearcherService postSearcher,
            PostCreatorService postCreator,
            SemanticRouterService semanticRouterService,
            TrendingTopicsAnalyzer trendingTopicsAnalyzer, PostSummarizer postSummarizer,
            OpenAiChatModel openAiChatModel, SemanticCacheService semanticCacheService, BloomFilterService bloomFilterService) {
        this.authService = authService;
        this.postSearcher = postSearcher;
        this.postCreator = postCreator;
        this.semanticRouterService = semanticRouterService;
        this.trendingTopicsAnalyzer = trendingTopicsAnalyzer;
        this.postSummarizer = postSummarizer;
        this.openAiChatModel = openAiChatModel;
        this.semanticCacheService = semanticCacheService;
        this.bloomFilterService = bloomFilterService;
    }

    @Scheduled(fixedDelay = 30000)
    public void runBot() {
        run();
    }

    public void run() {
        try {
            String accessToken = authService.getAccessToken();
            List<PostSearcherService.Post> posts = postSearcher.searchPosts(
                "@devbubble.bsky.social", 15, accessToken
            );

            for (PostSearcherService.Post post : posts) {
                // Implement deduplication using Bloom Filter here

                String originalText = post.getRecord().getText();
                String cleanedText = originalText.replace("@devbubble.bsky.social", "").trim();
                String handle = post.getAuthor().getHandle();

                String reply = "@" + handle + " " + processUserRequest(cleanedText);
                List<String> chunks = postCreator.splitIntoChunks(reply, 300);

                for (String chunk : chunks) {
                    postCreator.createPost(
                        accessToken,
                        did,
                        chunk,
                        post.getUri(),
                        post.getCid()
                    );
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Error running bot: " + e.getMessage());
        }
    }

    public String processUserRequest(String userPost) {
        Set<String> matchedRoutes = semanticRouterService.matchRoute(userPost);
        logger.info("Matched routes: {}", matchedRoutes);

        // Get data based on the matched routes
        List<String> enrichedData = matchedRoutes.stream()
            .flatMap(route -> switch (route) {
                case "trending_topics" -> trendingTopicsAnalyzer.getTrendingTopics().stream();
                case "summarization" -> postSummarizer.summarizePosts(userPost).stream();
                default -> {
                    logger.warn("No handler for route: {}", route);
                    yield Stream.of("");
                }
            }).toList();

        logger.info("Enriched data: {}", enrichedData.toString());

        // Generate a response using the LLM
        String systemPrompt = "You are a bot that helps users analyse posts about politics. " +
            "You may be given a data set to help you answer questions. " +
            "Answer in a max of 300 chars. I MEAN IT. It's a TWEET. " +
            "Don't write more than 300 chars. Respond in only ONE paragraph. " +
            "Be as concise as possible";

        List<Message> messages = List.of(
            new SystemMessage(systemPrompt),
            new SystemMessage("Enriching data: " + enrichedData),
            new UserMessage("User query: " + userPost)
        );

        Prompt prompt = new Prompt(messages);
        return openAiChatModel.call(prompt).getResult().getOutput().getText();
    }
}