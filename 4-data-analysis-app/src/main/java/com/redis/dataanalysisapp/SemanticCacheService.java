package com.redis.dataanalysisapp;

import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.tuple.Fields;
import com.redis.om.spring.tuple.Pair;
import com.redis.om.spring.vectorize.Embedder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SemanticCacheService {

    private final SemanticCacheRepository repository;
    private final Embedder embedder;
    private final EntityStream entityStream;

    public SemanticCacheService(SemanticCacheRepository repository, Embedder embedder, EntityStream entityStream) {
        this.repository = repository;
        this.embedder = embedder;
        this.entityStream = entityStream;
    }

    public void insertIntoCache(String post, String answer) {
        SemanticCacheEntry entry = new SemanticCacheEntry(post, answer);
        repository.save(entry);
    }

    public String getFromCache(String post) {
        byte[] embedding = embedder.getTextEmbeddingsAsBytes(List.of(post), SemanticCacheEntry$.POST).getFirst();
        List<Pair<SemanticCacheEntry, Double>> scores = entityStream.of(SemanticCacheEntry.class)
            .filter(SemanticCacheEntry$.POST_EMBEDDING.knn(1, embedding))
            .map(Fields.of(SemanticCacheEntry$._THIS, SemanticCacheEntry$._POST_EMBEDDING_SCORE))
            .collect(Collectors.toList());

        return scores.stream()
            .filter(it -> it.getSecond() < 0.2)
            .findFirst()
            .map(it -> it.getFirst().getAnswer())
            .orElse("");
    }
}