package com.redis.dataanalysisapp;

import com.redis.om.spring.annotations.*;
import com.redis.om.spring.indexing.DistanceMetric;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@IndexingOptions(indexName = "SemanticCacheIdx")
@RedisHash
public class SemanticCacheEntry {

    @Id
    private String id;

    // Annotation to indicate that this field should be vectorized using OpenAI's embedding model
    @Vectorize(
        destination = "textEmbedding",
        provider = EmbeddingProvider.OPENAI,
        openAiEmbeddingModel = OpenAiApi.EmbeddingModel.TEXT_EMBEDDING_3_LARGE
    )
    private String post;

    // Annotation to indicate that this field should be indexed as a vector
    @VectorIndexed(
        dimension = 3072,
        distanceMetric = DistanceMetric.COSINE
    )
    private byte[] postEmbedding;

    private String answer;

    public SemanticCacheEntry(String post, String answer) {
        this.post = post;
        this.answer = answer;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPost() {
        return post;
    }

    public void setPost(String post) {
        this.post = post;
    }

    public byte[] getPostEmbedding() {
        return postEmbedding;
    }

    public void setPostEmbedding(byte[] postEmbedding) {
        this.postEmbedding = postEmbedding;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}