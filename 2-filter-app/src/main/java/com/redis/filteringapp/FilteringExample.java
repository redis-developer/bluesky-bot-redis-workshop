package com.redis.filteringapp;

import com.redis.om.spring.annotations.VectorIndexed;
import com.redis.om.spring.annotations.Vectorize;
import com.redis.om.spring.indexing.DistanceMetric;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value="FilteringExample")
public class FilteringExample {
    @Id
    private String id;

    // Implement the Vectorize annotation to specify the model to use for embedding
    private String text;

    // Use VectorIndexed to enable vector indexing on the textEmbedding field
    private byte[] textEmbedding;

    public FilteringExample() {
    }

    public FilteringExample(String reference) {
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
}