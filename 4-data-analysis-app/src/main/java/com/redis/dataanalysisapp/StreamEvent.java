package com.redis.dataanalysisapp;

import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.IndexingOptions;
import com.redis.om.spring.annotations.VectorIndexed;
import com.redis.om.spring.annotations.Vectorize;
import com.redis.om.spring.indexing.DistanceMetric;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.redis.core.RedisHash;
import redis.clients.jedis.resps.StreamEntry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@IndexingOptions(indexName = "StreamEventIdx")
@RedisHash(value="StreamEvent")
public class StreamEvent {

    @Id
    private String id;
    private String did;
    private String rkey;
    private String text;

    @Vectorize(destination = "textEmbedding")
    private String textToEmbed;

    @VectorIndexed(distanceMetric = DistanceMetric.COSINE, dimension = 384)
    private byte[] textEmbedding;

    @Indexed
    private Long timeUs;
    private String operation;
    private String uri;
    private String parentUri;
    private String rootUri;

    @Indexed
    private List<String> langs;
    
    @Indexed
    private List<String> topics;

    @Transient
    private String redisStreamEntryId;

    public StreamEvent(String id, String did, String rkey, String text, Long timeUs,
                       String operation, String uri, String parentUri,
                       String rootUri, List<String> langs, String redisStreamEntryId) {
        this.id = id;
        this.did = did;
        this.rkey = rkey;
        this.text = text;
        this.timeUs = timeUs;
        this.operation = operation;
        this.uri = uri;
        this.parentUri = parentUri;
        this.rootUri = rootUri;
        this.langs = langs;
        this.redisStreamEntryId = redisStreamEntryId;
    }

    public static StreamEvent fromStreamEntry(StreamEntry entry) {
        Map<String, String> fields = entry.getFields();

        String langsStr = fields.getOrDefault("langs", "[]");
        List<String> langs = Arrays.asList(
            langsStr.replace("[", "").replace("]", "").split(", ")
        );

        return new StreamEvent(
                fields.getOrDefault("uri", ""), // ID
                fields.getOrDefault("did", ""),
                fields.getOrDefault("rkey", ""),
                fields.getOrDefault("text", ""),
                Long.parseLong(fields.getOrDefault("timeUs", "0")),
                fields.getOrDefault("operation", ""),
                fields.getOrDefault("uri", ""),
                fields.getOrDefault("parentUri", ""),
                fields.getOrDefault("rootUri", ""),
                langs,
                entry.getID().toString()
        );
    }

    // Convert to Map for Redis Stream
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("did", this.did);
        map.put("rkey", this.rkey);
        map.put("text", this.text);
        map.put("timeUs", this.timeUs.toString());
        map.put("operation", this.operation);
        map.put("uri", this.uri);
        map.put("parentUri", this.parentUri);
        map.put("rootUri", this.rootUri);
        map.put("langs", this.langs.toString());
        return map;
    }

    // Getters
    public String getId() { return id; }
    public String getText() { return text; }
    public String getOperation() { return operation; }
    public String getUri() { return uri; }
    public String getDid() {return did;}
    public String getRkey() {return rkey;}
    public String getTextToEmbed() {return textToEmbed;}
    public byte[] getTextEmbedding() {return textEmbedding;}
    public Long getTimeUs() {return timeUs;}
    public String getParentUri() {return parentUri;}
    public String getRootUri() {return rootUri;}
    public List<String> getLangs() {return langs;}
    public List<String> getTopics() {return topics;}

    // Setters
    public void setTextToEmbed(String textToEmbed) {
        this.textToEmbed = textToEmbed;
    }

    public void setTextEmbedding(byte[] textEmbedding) {
        this.textEmbedding = textEmbedding;
    }
    
    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public String getRedisStreamEntryId() {
        return redisStreamEntryId;
    }
}