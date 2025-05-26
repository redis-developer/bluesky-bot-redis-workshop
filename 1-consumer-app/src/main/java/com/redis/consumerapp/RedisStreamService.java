package com.redis.consumerapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.XAddParams;

import java.util.Map;

@Service
public class RedisStreamService {

    private final static Logger logger = LoggerFactory.getLogger(RedisStreamService.class);
    private final JedisPooled jedisPooled;

    public RedisStreamService(JedisPooled jedisPooled) {
        this.jedisPooled = jedisPooled;
    }

    public void addToStream(String streamName, Map<String, String> hash) {
        // Implement the XADD command to add a new entry to the stream
    }
}
