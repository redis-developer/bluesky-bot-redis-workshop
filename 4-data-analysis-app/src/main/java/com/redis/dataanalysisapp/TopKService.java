package com.redis.dataanalysisapp;

import com.redis.om.spring.ops.pds.TopKOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TopKService {
    private final Logger logger = LoggerFactory.getLogger(TopKService.class);
    private final TopKOperations<String> opsForTopK;

    public TopKService(TopKOperations<String> opsForTopK) {
        this.opsForTopK = opsForTopK;
    }

    public List<String> topK(String topKName) {
        return opsForTopK.list(topKName);
    }
}
