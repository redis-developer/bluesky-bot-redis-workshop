package com.redis.topicextractorapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import redis.clients.jedis.JedisPooled;

import java.util.*;

@Service
public class TopicExtractionService {
    private static final Logger logger = LoggerFactory.getLogger(TopicExtractionService.class);
    private final JedisPooled jedis;
    private final OllamaChatModel chatModel;

    public TopicExtractionService(JedisPooled jedisPooled, OllamaChatModel chatModel, CountMinSketchService countMinSketchService) {
        this.jedis = jedisPooled;
        this.chatModel = chatModel;
    }

    private String extractTopics(String post, String existingTopics) {
        List<Message> messages = List.of(
                new SystemMessage(PROMPT),
                new UserMessage("Existing topics: " + existingTopics),
                new UserMessage("Post: " + post)
        );

        Prompt prompt = new Prompt(messages);
        ChatResponse response = chatModel.call(prompt);

        return response.getResult().getOutput().getText() != null
                ? response.getResult().getOutput().getText()
                : "";
    }

    public List<String> processTopics(StreamEvent event) {
            Set<String> existingTopics = jedis.smembers("topics");

            String result = extractTopics(
                    event.getText(),
                    String.join(", ", existingTopics)
            );

            List<String> topics = Arrays.stream(result
                            .replace("\"", "")
                            .replace("“", "")
                            .replace("”", "")
                            .split(","))
                    .map(String::trim)
                    .filter(t -> !t.isEmpty())
                    .toList();

            jedis.sadd("topics", topics.toArray(new String[0]));

            return topics;
        }

    private static final String PROMPT = """
            You are a topic classifier specialized in politics. Given a post, extract only politics-related topics—both explicitly mentioned and reasonably implied.
            
            If a post mentions a political figure, event, party, law, or movement, infer related political topics or domains.
            
            For example, if the post mentions “Green New Deal”, you may infer topics like “climate policy”, “progressive politics”, and “US Congress”.
            
            Avoid generic terms like “news”, “statement”, or “speech”.
            Only return relevant political topics.
            
            Also avoid overly narrow items such as specific bill numbers or individual quotes.
            
            If the topic or a very similar is already in the provided list of existing topics, use the one from the list, otherwise, feel free to create a new one.
            
            If the content is not political, return an empty string.
            
            Format your response as comma separated values (ALWAYS, I MEAN IT):
            "topic1, topic2, topic3"
            
            Examples:
            
            Post:
            Climate change policy needs serious bipartisan commitment.
            Output:
            “Climate Policy, Bipartisanship, Environmental Politics”
            ⸻
            Post:
            Macron’s recent comments on NATO expansion are causing waves.
            Output:
            “Emmanuel Macron, NATO, Foreign Policy, European Politics”
            ⸻
            Post:
            Just watched a debate on universal basic income — fascinating stuff!
            Output:
            “Universal Basic Income, Economic Policy, Social Welfare”
            ⸻
            Post:
            The Supreme Court decision today is a major turning point.
            Output:
            “Supreme Court, Judicial System, Constitutional Law”
            ⸻
            Post:
            Alexandria Ocasio-Cortez is pushing for stronger climate legislation.
            Output:
            “Alexandria Ocasio-Cortez, Climate Policy, Progressive Politics, US Congress”
            -
            Post:
            The Nintendo Switch is a cool video game console!
            Output:
            ""
            """;
}
