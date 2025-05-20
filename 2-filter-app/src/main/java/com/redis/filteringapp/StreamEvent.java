package com.redis.filteringapp;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import redis.clients.jedis.resps.StreamEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RedisHash(value="StreamEvent")
public class StreamEvent {

    @Id
    private String id;
    private String did;
    private String rkey;
    private String text;
    private Long timeUs;
    private String operation;
    private String uri;
    private String parentUri;
    private String rootUri;
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
    public String getText() { return text; }
    public String getOperation() { return operation; }
    public String getUri() { return uri; }
}