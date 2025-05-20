package com.redis.consumerapp;

import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.XAddParams;

import java.util.Map;

@Service
public class RedisStreamService {

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

}
