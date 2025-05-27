package com.redis.dataanalysisapp;

import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrendingTopicsAnalyzer {
    private final JedisPooled jedisPooled;
    private final TopKService topKService;

    public TrendingTopicsAnalyzer(JedisPooled jedisPooled, TopKService topKService) {
        this.jedisPooled = new JedisPooled();
        this.topKService = topKService;
    }

    public List<String> getTrendingTopics() {
        // Implement logic to retrieve trending topics
        String currentMinute = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).toString();
        return topKService.topK("topics-topk:" + currentMinute);
    }
}