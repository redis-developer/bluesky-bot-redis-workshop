package com.redis.filteringapp;

import com.redis.om.spring.repository.RedisDocumentRepository;

public interface StreamEventRepository extends RedisDocumentRepository<StreamEvent, String> {
}
