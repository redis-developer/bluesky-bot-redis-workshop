package com.redis.vectorembeddings;

import com.redis.om.spring.repository.RedisDocumentRepository;

public interface StreamEventRepository extends RedisDocumentRepository<StreamEvent, String> {
}