package com.redis.filteringapp;

import com.redis.om.spring.repository.RedisEnhancedRepository;

public interface FilteringExampleRepository extends RedisEnhancedRepository<FilteringExample, String> {
}
