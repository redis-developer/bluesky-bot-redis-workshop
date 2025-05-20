package com.redis.filteringapp;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.VectorIndexed;
import com.redis.om.spring.annotations.Vectorize;
import com.redis.om.spring.indexing.DistanceMetric;
import org.springframework.data.annotation.Id;
import redis.clients.jedis.resps.StreamEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document
public class StreamEvent {

    @Id
    private String id;
    private String did;
    private String rkey;

    @Vectorize(destination = "textEmbedding")
    private String text;

    @VectorIndexed(distanceMetric = DistanceMetric.COSINE, dimension = 384)
    private float[] textEmbedding;

    @Indexed
    private Long timeUs;
    private String operation;
    private String uri;
    private String parentUri;
    private String rootUri;

    @Indexed
    private List<String> langs;

    public StreamEvent(String id, String did, String rkey, String text, Long timeUs,
                      String operation, String uri, String parentUri, 
                      String rootUri, List<String> langs) {
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
                langs
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
    public String getDid() { return did; }
    public String getRkey() { return rkey; }
    public String getText() { return text; }
    public Long getTimeUs() { return timeUs; }
    public String getOperation() { return operation; }
    public String getUri() { return uri; }
    public String getParentUri() { return parentUri; }
    public String getRootUri() { return rootUri; }
    public List<String> getLangs() { return langs; }
}