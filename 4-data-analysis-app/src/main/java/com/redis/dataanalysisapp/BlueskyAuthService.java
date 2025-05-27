package com.redis.dataanalysisapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.JedisPooled;

import java.util.Map;
import java.util.Objects;

@Service
public class BlueskyAuthService {

    private final RestTemplate restTemplate;

    @Value("${bluesky.username}")
    private String username;

    @Value("${bluesky.token}")
    private String password;

    public BlueskyAuthService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoginResponse {
        @JsonProperty("accessJwt")
        private String accessJwt;

        @JsonProperty("refreshJwt")
        private String refreshJwt;

        @JsonProperty("handle")
        private String handle;

        @JsonProperty("did")
        private String did;

        public String getAccessJwt() { return accessJwt; }
        public String getRefreshJwt() { return refreshJwt; }
        public String getHandle() { return handle; }
        public String getDid() { return did; }
    }


    public String getAccessToken() {
        Map<String, String> payload = Map.of(
                "identifier", username,
                "password", password
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "https://bsky.social/xrpc/com.atproto.server.createSession",
                entity,
                LoginResponse.class
        );

        return Objects.requireNonNull(response.getBody()).getAccessJwt();
    }
}