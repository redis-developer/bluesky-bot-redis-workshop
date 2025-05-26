package com.redis.dataanalysisapp;

import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.tuple.Fields;
import com.redis.om.spring.tuple.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostSummarizer {
    private static final Logger logger = LoggerFactory.getLogger(PostSummarizer.class);
    private final JedisPooled jedisPooled;
    private final TopicExtractionService topicExtractionService;
    private final EntityStream entityStream;

    public PostSummarizer(TopicExtractionService topicExtractionService, EntityStream entityStream) {
        this.entityStream = entityStream;
        this.jedisPooled = new JedisPooled();
        this.topicExtractionService = topicExtractionService;
    }

    public List<String> summarizePosts(String userQuery) {
        // Get existing topics from Redis
        Set<String> existingTopics = jedisPooled.smembers("topics");

        // Extract topics from the user query
        List<String> queryTopics = topicExtractionService.extractTopics(userQuery);
        logger.info("Query topics: {}", queryTopics);

        // For each topic, search for posts in Redis
        return entityStream.of(StreamEvent.class)
                .filter(StreamEvent$.TOPICS.eq(queryTopics))
                .map(Fields.of(StreamEvent$.TEXT))
                .collect(Collectors.toList())
                .stream()
                .map(Single::getFirst)
                .toList();
    }
}