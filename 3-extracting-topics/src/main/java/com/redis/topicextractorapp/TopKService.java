package com.redis.topicextractorapp;

import com.redis.om.spring.ops.pds.TopKOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.List;
import java.util.Map;

@Service
public class TopKService {
    private final Logger logger = LoggerFactory.getLogger(TopKService.class);
    private final TopKOperations<String> opsForTopK;

    public TopKService(TopKOperations<String> opsForTopK) {
        this.opsForTopK = opsForTopK;
    }

    public void create(String name) {
        // Implementation of create method
    }

    public List<String> incrBy(String topKName, Map<String, Long> counters) {
        // Implementation of incrBy method
        return null;
    }
}
