package com.redis.dataanalysisapp;

import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.tuple.Fields;
import com.redis.om.spring.tuple.Pair;
import com.redis.om.spring.vectorize.Embedder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SemanticRouterService {
    private static final Logger logger = LoggerFactory.getLogger(SemanticRouterService.class);
    private final Embedder embedder;
    private final EntityStream entityStream;
    private final RoutingRepository repository;

    public SemanticRouterService(Embedder embedder, EntityStream entityStream, RoutingRepository repository) {
        this.embedder = embedder;
        this.entityStream = entityStream;
        this.repository = repository;
    }

    boolean areReferencesLoaded() {
        return repository.count() > 0;
    }

    void loadReferences(List<String> references, String route, double maxThreshold) {
        references.stream()
            .map(reference -> {
                Routing routing = new Routing();
                routing.setRoute(route);
                routing.setMinThreshold(maxThreshold);
                routing.setText(reference);
                return routing;
            }).forEach(repository::save);
    }

    private byte[] createEmbedding(String text) {
        // Implement embedding creation logic
        return embedder.getTextEmbeddingsAsBytes(List.of(text), Routing$.TEXT).getFirst();
    }

    private Pair<Routing, Double> vectorSimilaritySearch(byte[] embedding) {
        // Implement vector similarity search logic
        return null;
    }

    public Set<String> matchRoute(String post) {
        List<String> clauses = breakSentenceIntoClauses(post);

        // Implement logic to match the route based on the clauses
        return null;
    }

    private List<String> breakSentenceIntoClauses(String sentence) {
        return Arrays.stream(
            sentence.split("[!?,.:;()\"\\[\\]{}]+")
        ).filter(s -> !s.isBlank())
         .map(String::trim)
         .collect(Collectors.toList());
    }
}