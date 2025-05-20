package com.redis.topicextractorapp;

import com.redis.om.spring.ops.pds.CountMinSketchOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.List;
import java.util.Map;

@Service
public class CountMinSketchService {
    private final Logger logger = LoggerFactory.getLogger(CountMinSketchService.class);
    private final CountMinSketchOperations<String> opsForCms;

    public CountMinSketchService(CountMinSketchOperations<String> opsForCms) {
        this.opsForCms = opsForCms;
    }

    public void create(String name) {
        try {
            opsForCms.cmsInitByDim(name, 3000, 10);
        } catch(JedisDataException e) {
            logger.info("Count-min Sketch {} already exists", name);
        }
    }

    public List<Long> incrBy(String countMinSketchName, String value, long count) {
        return opsForCms.cmsIncrBy(countMinSketchName, Map.of(value, count));
    }

    public List<Long> incrBy(String countMinSketchName, Map<String, Long> counters) {
        return opsForCms.cmsIncrBy(countMinSketchName, counters);
    }

    public Long query(String countMinSketchName, String value) {
        return opsForCms.cmsQuery(countMinSketchName, value).stream().findFirst().orElse(0L);
    }
}
