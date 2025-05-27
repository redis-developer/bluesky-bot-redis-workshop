package com.redis.dataanalysisapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class PostSearcherService {

    private static final Logger logger = LoggerFactory.getLogger(PostSearcherService.class);
    private final RestTemplate restTemplate;

    public PostSearcherService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchResponse {
        @JsonProperty("cursor")
        private String cursor;

        @JsonProperty("hitsTotal")
        private Integer hitsTotal;

        @JsonProperty("posts")
        private List<Post> posts;

        public String getCursor() { return cursor; }
        public Integer getHitsTotal() { return hitsTotal; }
        public List<Post> getPosts() { return posts; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Post {
        @JsonProperty("uri")
        private String uri;

        @JsonProperty("cid")
        private String cid;

        @JsonProperty("author")
        private Author author;

        @JsonProperty("record")
        private Record record;

        public String getUri() { return uri; }
        public String getCid() { return cid; }
        public Author getAuthor() { return author; }
        public Record getRecord() { return record; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Author {
        @JsonProperty("did")
        private String did;

        @JsonProperty("handle")
        private String handle;

        @JsonProperty("displayName")
        private String displayName;

        public String getDid() { return did; }
        public String getHandle() { return handle; }
        public String getDisplayName() { return displayName; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Record {
        @JsonProperty("text")
        private String text;

        @JsonProperty("createdAt")
        private String createdAt;

        public String getText() { return text; }
        public String getCreatedAt() { return createdAt; }
    }

    public List<Post> searchPosts(String term, int hours, String accessToken) {
        List<Post> allPosts = new ArrayList<>();
        String cursor = null;
        String sinceTime = Instant.now().minus(hours, ChronoUnit.HOURS).toString();

        logger.info("🔍 Searching posts with tag: " + term + " since: " + sinceTime);

        do {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl("https://bsky.social/xrpc/app.bsky.feed.searchPosts")
                    .queryParam("q", term)
                    .queryParam("sort", "latest")
                    .queryParam("limit", "100")
                    .queryParam("since", sinceTime);

            if (cursor != null) {
                builder.queryParam("cursor", cursor);
            }

            var headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(accessToken);

            var entity = new org.springframework.http.HttpEntity<>(headers);

            var response = restTemplate.exchange(
                    builder.toUriString(),
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    SearchResponse.class
            );

            SearchResponse body = response.getBody();

            if (body != null && body.getPosts() != null) {
                allPosts.addAll(body.getPosts());
                logger.info("✅ Retrieved " + body.getPosts().size() +
                        " posts. Total so far: " + allPosts.size());
                cursor = body.getCursor();
            } else {
                break;
            }

        } while (cursor != null);

        logger.info("\uD83C\uDF89 Finished fetching posts. Total: {}", allPosts.size());
        return allPosts;
    }
}