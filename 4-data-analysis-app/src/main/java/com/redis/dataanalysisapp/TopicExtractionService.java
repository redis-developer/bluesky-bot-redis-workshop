package com.redis.dataanalysisapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
public class TopicExtractionService {
    private static final Logger logger = LoggerFactory.getLogger(TopicExtractionService.class);
    private final JedisPooled jedis;
    private final OpenAiChatModel chatModel;

    public TopicExtractionService(JedisPooled jedisPooled, OpenAiChatModel chatModel) {
        this.jedis = jedisPooled;
        this.chatModel = chatModel;
    }

    public List<String> extractTopics(String post) {
        Set<String> existingTopics = jedis.smembers("topics");
        List<Message> messages = List.of(
                new SystemMessage(PROMPT),
                new UserMessage("Existing topics: " + existingTopics),
                new UserMessage("Post: " + post)
        );

        Prompt prompt = new Prompt(messages);
        ChatResponse response = chatModel.call(prompt);

        String topics = response.getResult().getOutput().getText() != null
                ? response.getResult().getOutput().getText()
                : "";

        return Arrays.stream(topics
                        .replace("\"", "")
                        .split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .toList();
    }

    private static final String PROMPT = """
        You are a topic classifier specialized in artificial intelligence. Given a post, extract only AI-related topics—both explicitly mentioned and reasonably implied.
        
        If a post mentions an AI model, framework, technique, company, use case, research area, or tool, infer related AI topics or domains.
        
        For example, if the post mentions "LangChain and OpenAI APIs", you may infer topics like "Prompt Engineering", "Retrieval-Augmented Generation", and "AI Tooling".
        
        Avoid generic terms like "tech", "news", or "cool project".
        
        Only return relevant AI topics.
        
        Also avoid overly narrow items such as specific model version numbers or isolated API methods.
        
        If the topic or a very similar one is already in the provided list of existing topics, use the one from the list. Otherwise, feel free to create a new one.
        
        If the content is not related to AI at all, return an empty string.
        
        If the content still mentions AI, try to imply topics anyway.
        
        Format your response as comma separated values (ALWAYS, I MEAN IT):
        "topic1, topic2, topic3"
        
        ⸻
        
        Examples:
        
        Post:
        Just finished a tutorial on LangChain using OpenAI’s API. Super fun.
        Output:
        "LangChain, OpenAI, Prompt Engineering, AI Tooling"
        
        Post:
        Trying to run Mistral locally with Ollama. Inference seems fast!
        Output:
        "Mistral, Local Inference, Model Deployment, Open-Source LLMs"
        
        Post:
        Google’s new image model can generate photos from text prompts.
        Output:
        "Text-to-Image, Generative Models, Google AI, Diffusion Models"
        
        Post:
        Tried the new Zelda game over the weekend. It’s amazing!
        Output:
        ""
        """;
}
