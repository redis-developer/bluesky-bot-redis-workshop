package com.redis.dataanalysisapp;

import com.redis.om.spring.repository.RedisEnhancedRepository;

public interface SemanticCacheRepository extends RedisEnhancedRepository<SemanticCacheEntry, String> {
}