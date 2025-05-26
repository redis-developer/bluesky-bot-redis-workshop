package com.redis.dataanalysisapp;

import com.redis.om.spring.repository.RedisEnhancedRepository;

public interface RoutingRepository extends RedisEnhancedRepository<Routing, String> {
}
