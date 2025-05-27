package com.redis.dataanalysisapp;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PostCreatorService {

    private static final Logger logger = LoggerFactory.getLogger(PostCreatorService.class);
    
    private final RestTemplate restTemplate;

    public PostCreatorService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public static class ReplyRef {
        @JsonProperty("root")
        private PostRef root;

        @JsonProperty("parent")
        private PostRef parent;

        public ReplyRef(PostRef root, PostRef parent) {
            this.root = root;
            this.parent = parent;
        }
    }

    public static class PostRef {
        @JsonProperty("cid")
        private String cid;

        @JsonProperty("uri")
        private String uri;

        public PostRef(String cid, String uri) {
            this.cid = cid;
            this.uri = uri;
        }
    }

    public static class PostRecord {
        @JsonProperty("$type")
        private String type = "app.bsky.feed.post";

        @JsonProperty("text")
        private String text;

        @JsonProperty("createdAt")
        private String createdAt;

        @JsonProperty("reply")
        private ReplyRef reply;

        public PostRecord(String text, String createdAt, ReplyRef reply) {
            this.text = text;
            this.createdAt = createdAt;
            this.reply = reply;
        }
    }

    public static class PostRequest {
        @JsonProperty("repo")
        private String repo;

        @JsonProperty("collection")
        private String collection;

        @JsonProperty("record")
        private PostRecord record;

        public PostRequest(String repo, String collection, PostRecord record) {
            this.repo = repo;
            this.collection = collection;
            this.record = record;
        }
    }

    public boolean createPost(String accessToken, String repo, String text, String replyToUri, String replyToCid) {
        ReplyRef replyRef = null;

        if (replyToUri != null && replyToCid != null) {
            PostRef ref = new PostRef(replyToCid, replyToUri);
            replyRef = new ReplyRef(ref, ref);
        }

        PostRecord record = new PostRecord(text, Instant.now().toString(), replyRef);
        PostRequest postRequest = new PostRequest(repo, "app.bsky.feed.post", record);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<PostRequest> requestEntity = new HttpEntity<>(postRequest, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://bsky.social/xrpc/com.atproto.repo.createRecord",
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("✅ Post created{}!", replyRef != null ? " (as reply)" : "");
                return true;
            } else {
                logger.error("❌ Failed to create post: {}", response.getStatusCodeValue());
                logger.error(response.getBody());
                return false;
            }
        } catch (Exception e) {
            logger.error("❌ Exception while creating post: " + e.getMessage());
            return false;
        }
    }


    public List<String> splitIntoChunks(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() + word.length() + 1 > maxLength) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(word).append(' ');
        }

        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }
}