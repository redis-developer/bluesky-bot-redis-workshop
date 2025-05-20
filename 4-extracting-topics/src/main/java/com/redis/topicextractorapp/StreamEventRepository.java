package com.redis.topicextractorapp;

import com.redis.om.spring.repository.RedisEnhancedRepository;

public interface StreamEventRepository extends RedisEnhancedRepository<StreamEvent, String> {
}