package com.redis.dataanalysisapp;

import org.springframework.data.domain.Sort.Direction;
import com.redis.om.spring.repository.query.Sort;
import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.search.stream.predicates.tag.EqualPredicate;
import com.redis.om.spring.tuple.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
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
        // Extract topics from the user query
        List<String> queryTopics = topicExtractionService.extractTopics(userQuery);
        logger.info("Query topics: {}", queryTopics);

        // For each topic, search for posts in Redis
        List<EqualPredicate<StreamEvent, List<String>>> predicates = queryTopics.stream().map(
            topic -> StreamEvent$.TOPICS.eq(List.of(topic))
        ).toList();

        Predicate<List<String>> finalPredicate = predicates.getFirst();

        for (int i = 1; i < predicates.size(); i++) {
            finalPredicate = finalPredicate.or(predicates.get(i));
        }

        return entityStream.of(StreamEvent.class)
            .filter(finalPredicate)
            .map(Fields.of(StreamEvent$._THIS, StreamEvent$._THIS))
            .collect(Collectors.toList())
            .stream()
            .map(pair -> pair.getSecond().getText())
            .toList();
    }
}