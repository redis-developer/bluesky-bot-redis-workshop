# Building a Bluesky Bot with Java and Redis

## Introduction

Welcome to this workshop on building a Bluesky social network bot using Java and Redis! In this workshop, you'll learn how to build a complete bot that can analyze posts from the Bluesky network, identify trending topics, and respond to user queries about the content.

You'll learn how to use Redis as the core data platform alongside vector search and Large Language Models (LLMs) to build an intelligent data processing pipeline.

## Prerequisites

To follow this workshop, you'll need:

- Basic knowledge of Java
- Docker for running Redis Open Source 8 (which includes Redis Query Engine and Probabilistic Data Structures)
- An Ollama installation for running LLMs locally (or OpenAI API key)
- A Bluesky account for testing the bot
- Java 21 or higher
- Maven or Gradle for dependency management

## Getting Started

1. Clone the repository
2. Start Redis Open Source 8 using Docker:
   ```bash
   docker run --name my-redis -p 6379:6379 redis
   ```
   This provides Redis with all the necessary modules (Redis Query Engine, RedisJSON, etc.).
3. Install Redis Insight for visualizing the data: https://redis.io/docs/latest/operate/redisinsight/install/
4. Verify Redis is running:
   ```bash
   docker exec -it my-redis redis-cli ping
   ```
   This should return "PONG".
5. Start Ollama with the Deepseek Coder model:
   ```bash
   ollama run deepseek-coder-v2
   ```
6. Set up your Java project with the required dependencies (see below)

## Required Dependencies

For this workshop, you'll need the following dependencies:

```kotlin
dependencies {
   // Redis OM Spring
   implementation("com.redis.om:redis-om-spring:1.0.0-RC1")
   implementation("com.redis.om:redis-om-spring-ai:1.0.0-RC1")
    
   // Spring AI with Ollama
   implementation("org.springframework.ai:spring-ai-ollama:1.0.0-RC1")

   // DJL for machine learning
   implementation("ai.djl:api:0.33.0")
   implementation("ai.djl.huggingface:tokenizers:0.33.0")
   implementation("ai.djl.pytorch:pytorch-engine:0.33.0")
}
```

## Workshop Overview

This workshop is divided into five parts, each building on the previous one:

1. **JetStream Consumer**: Connect to Bluesky's Jetstream Websocket and store events in Redis Streams
2. **JetStream Filtering**: Filter events using Redis Bloom Filter and machine learning
3. **Events Enrichment**: Enrich events with topic modeling and vector embeddings
4. **Data Analysis with AI**: Build a question-answering system using Redis and LLMs
5. **Building the Bot**: Create a bot that interacts with users on Bluesky

Let's get started!

## Part 1: Consuming Bluesky's Jetstream Websocket

In this part, we'll connect to Bluesky's Jetstream Websocket to receive real-time events and store them in Redis Streams.

### Understanding Bluesky's Jetstream and Redis Streams

Bluesky's Jetstream Websocket provides a stream of events from the Bluesky network. These events include posts, likes, follows, and other user activities. Redis Streams are a data structure that allows you to store and consume a stream of events, similar to Kafka topics but simpler to use.

### Required dependencies

```kotlin
dependencies {
   // Redis OM Spring
   implementation("com.redis.om:redis-om-spring:1.0.0-RC1")
   implementation("com.redis.om:redis-om-spring-ai:1.0.0-RC1")
}
```

### Modeling the Event Data

First, let's create a model to represent the events from Bluesky's Jetstream:

```java
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
```

### Creating a WebSocket Client

Now, let's create a WebSocket client to connect to Bluesky's Jetstream:

```java
package com.redis.consumerapp;

import jakarta.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
@ClientEndpoint
public class JetstreamClient {

   private static final Logger logger = LoggerFactory.getLogger(JetstreamClient.class);

   private Session session;
   private URI endpointURI;
   private boolean manuallyClosed = false;

   @OnOpen
   public void onOpen(Session session) {
      System.out.println("✅ Connected to Jetstream");
   }

   @OnMessage
   public void onMessage(String message) {
      System.out.println("🔔 Received message: " + message);
      // TODO: parse message into Event and process
   }

   @OnClose
   public void onClose(Session session, CloseReason closeReason) {
      logger.info("Disconnected: " + closeReason);
      if (!manuallyClosed) {
         tryReconnect();
      }
   }

   @OnError
   public void onError(Session session, Throwable throwable) {
      System.err.println("WebSocket error: " + throwable.getMessage());
      if (!manuallyClosed) {
         tryReconnect();
      }
   }

   public void start() throws Exception {
      start("wss://jetstream2.us-east.bsky.network/subscribe?wantedCollections=app.bsky.feed.post");
   }

   public void start(String uri) throws Exception {
      this.endpointURI = new URI(uri);
      connect();
   }

   private void connect() throws Exception {
      WebSocketContainer container = ContainerProvider.getWebSocketContainer();
      this.session = container.connectToServer(this, endpointURI);
   }

   private void tryReconnect() {
      new Thread(() -> {
         int attempts = 0;
         while (!manuallyClosed) {
            try {
               Thread.sleep(Math.min(30000, 2000 * ++attempts)); // exponential up to 30s
               logger.info("Trying to reconnect... attempt " + attempts);
               connect();
               logger.info("Reconnected!");
               break;
            } catch (Exception e) {
               System.err.println("Reconnect failed: " + e.getMessage());
            }
         }
      }).start();
   }

   public void stop() throws IOException {
      manuallyClosed = true;
      if (session != null && session.isOpen()) {
         session.close();
      }
   }
}
```

### Adding Events to Redis Streams

Now, let's create a function to add events to a Redis Stream:

```java
package com.redis.consumerapp;

import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.XAddParams;

import java.util.Map;

@Service
public class RedisStreamService {

   private final JedisPooled jedisPooled;

   public RedisStreamService(JedisPooled jedisPooled) {
      this.jedisPooled = jedisPooled;
   }

   public void addToStream(String streamName, Map<String, String> hash) {
      jedisPooled.xadd(
              streamName,
              XAddParams.xAddParams()
                      .id(StreamEntryID.NEW_ENTRY)
                      .maxLen(1_000_000)
                      .exactTrimming(),
              hash
      );
   }

}
```

### Putting It All Together

Finally, let's create a main class to run our Jetstream consumer:

```java
package com.redis.consumerapp;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.JedisPooled;

@SpringBootApplication
public class Application {

   public static void main(String[] args) {
      SpringApplication.run(Application.class, args);
   }

   @Bean
   public JedisPooled jedisPooled() {
      return new JedisPooled();
   }

   @Bean
   public CommandLineRunner run(
           JetstreamClient client,
           RedisStreamService redisStreamService) {
      return args -> {
         client.setMessageConsumer(message -> {
            BlueskyEvent blueskyEvent = BlueskyEvent.fromJson(message);
            redisStreamService.addToStream("jetstream", blueskyEvent.toMap());
         });
         client.start();
      };
   }
}
```
