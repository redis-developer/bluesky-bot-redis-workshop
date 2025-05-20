package com.redis.topicextractorapp;

import com.redis.om.spring.ops.pds.BloomOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.exceptions.JedisDataException;

@Service
public class BloomFilterService {
    private final Logger logger = LoggerFactory.getLogger(BloomFilterService.class);
    private final BloomOperations<String> opsForBloom;

    public BloomFilterService(BloomOperations<String> opsForBloom) {
        this.opsForBloom = opsForBloom;
    }

    public void createBloomFilter(String name) {
        try {
            opsForBloom.createFilter(name, 1_000_000L, 0.01);
        } catch(JedisDataException e) {
            logger.info("Bloom filter {} already exists", name);
        }
    }

    public boolean isInBloomFilter(String bloomFilter, String value) {
        return opsForBloom.exists(bloomFilter, value);
    }

    public void addToBloomFilter(String bloomFilter, String value) {
        opsForBloom.add(bloomFilter, value);
    }
}
