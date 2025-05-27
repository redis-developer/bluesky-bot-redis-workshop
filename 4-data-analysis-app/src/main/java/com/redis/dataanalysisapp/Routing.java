package com.redis.dataanalysisapp;

import com.redis.om.spring.annotations.EmbeddingProvider;
import com.redis.om.spring.annotations.IndexingOptions;
import com.redis.om.spring.annotations.VectorIndexed;
import com.redis.om.spring.annotations.Vectorize;
import com.redis.om.spring.indexing.DistanceMetric;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@IndexingOptions(indexName = "RoutingIdx")
@RedisHash(value="Routing")
public class Routing {
    @Id
    private String id;

    // Annotation to indicate that this field should be vectorized using OpenAI's embedding model
    @Vectorize(
        destination = "textEmbedding",
        provider = EmbeddingProvider.OPENAI,
        openAiEmbeddingModel = OpenAiApi.EmbeddingModel.TEXT_EMBEDDING_3_LARGE
    )
    private String text;

    // Annotation to indicate that this field should be indexed as a vector
    @VectorIndexed(
        distanceMetric = DistanceMetric.COSINE,
        dimension = 3072
    )
    private byte[] textEmbedding;

    private String route;

    private Double minThreshold;

    public Routing() {
    }

    public Routing(String reference) {
        this.text = reference;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public byte[] getTextEmbedding() {
        return textEmbedding;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTextEmbedding(byte[] textEmbedding) {
        this.textEmbedding = textEmbedding;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public Double getMinThreshold() {
        return minThreshold;
    }

    public void setMinThreshold(Double minThreshold) {
        this.minThreshold = minThreshold;
    }
}