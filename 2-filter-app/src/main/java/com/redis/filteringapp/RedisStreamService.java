package com.redis.filteringapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.resps.StreamEntry;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RedisStreamService {

    private final static Logger logger = LoggerFactory.getLogger(RedisStreamService.class);

    private final JedisPooled jedisPooled;

    public RedisStreamService(JedisPooled jedisPooled) {
        this.jedisPooled = jedisPooled;
    }

    public void addToStream(String streamName, Map<String, String> hash) {
        jedisPooled.xadd(
                streamName,
                XAddParams.xAddParams()
                        .id(StreamEntryID.NEW_ENTRY)
                        .maxLen(1_000_000)
                        .exactTrimming(),
                hash
        );
    }

    public void acknowledgeMessage(
            String streamName,
            String consumerGroup,
            String streamEntryId) {
        jedisPooled.xack(streamName, consumerGroup, new StreamEntryID(streamEntryId));
    }

    public void createConsumerGroup(String streamName, String consumerGroupName) {
        // Implement the function to invoke XGROUP CREATE command
    }

    public List<Map.Entry<String, List<StreamEntry>>> readFromStream(
            String streamName,
            String consumerGroup,
            String consumer,
            int count) {
        // Implement the function to invoke XREADGROUP command
        return null;
    }
}
