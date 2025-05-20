package com.redis.vectorembeddings;

import com.redis.om.spring.repository.RedisEnhancedRepository;

public interface StreamEventRepository extends RedisEnhancedRepository<StreamEvent, String> {
}