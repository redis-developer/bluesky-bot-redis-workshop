package com.redis.consumerapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BlueskyEvent {
    public String did;

    @JsonProperty("time_us")
    public long timeUs;

    public String kind;
    public Commit commit;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Commit {
        public String rev;
        public String operation;
        public String collection;
        public String rkey;
        public Record record;
        public String cid;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Record {
        @JsonProperty("$type")
        public String type;

        public String createdAt;
        public String text;
        public List<String> langs;
        public List<Facet> facets;
        public Reply reply;
        public Embed embed;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Reply {
        public PostRef parent;
        public PostRef root;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PostRef {
        public String cid;
        public String uri;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Facet {
        @JsonProperty("$type")
        public String type;

        public List<Feature> features;
        public Index index;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Feature {
        @JsonProperty("$type")
        public String type;
        public String did;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Index {
        public int byteStart;
        public int byteEnd;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embed {
        @JsonProperty("$type")
        public String type;
        public List<EmbedImage> images;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbedImage {
        public String alt;
        public AspectRatio aspectRatio;
        public Image image;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AspectRatio {
        public int height;
        public int width;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image {
        @JsonProperty("$type")
        public String type;
        public Ref ref;
        public String mimeType;
        public int size;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ref {
        @JsonProperty("$link")
        public String link;
    }

    public static BlueskyEvent fromJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        BlueskyEvent event;
        try {
            event = mapper.readValue(json, BlueskyEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return event;
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("did", did);
        map.put("createdAt", commit != null && commit.record != null ? String.valueOf(commit.record.createdAt) : "");
        map.put("timeUs", String.valueOf(timeUs));
        map.put("text", commit != null && commit.record != null ? String.valueOf(commit.record.text) : "");
        map.put("langs", commit != null && commit.record != null ? String.valueOf(commit.record.langs) : "");
        map.put("operation", commit != null ? String.valueOf(commit.operation) : "");
        map.put("rkey", commit != null ? String.valueOf(commit.rkey) : "");
        map.put("parentUri", commit != null && commit.record != null && commit.record.reply != null && commit.record.reply.parent != null ? commit.record.reply.parent.uri : "");
        map.put("rootUri", commit != null && commit.record != null && commit.record.reply != null && commit.record.reply.root != null ? commit.record.reply.root.uri : "");
        map.put("uri", commit != null ? "at://" + did + "/app.bsky.feed.post/" + commit.rkey : "");
        return map;
    }
}
