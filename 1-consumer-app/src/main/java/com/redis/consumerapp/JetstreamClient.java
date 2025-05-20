package com.redis.consumerapp;

import jakarta.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

@Component
@ClientEndpoint
public class JetstreamClient {

    private static final Logger logger = LoggerFactory.getLogger(JetstreamClient.class);

    private Session session;
    private URI endpointURI;
    private boolean manuallyClosed = false;
    private Consumer<String> messageConsumer = null;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("✅ Connected to Jetstream");
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("🔔 Received message: " + message);
        if (messageConsumer != null) {
            messageConsumer.accept(message);
        }
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
                    logger.info("Trying to reconnect... attempt {}", attempts);
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

    public void setMessageConsumer(Consumer<String> messageConsumer) {
        this.messageConsumer = messageConsumer;
    }
}