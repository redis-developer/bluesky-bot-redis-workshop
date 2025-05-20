package com.redis.filteringapp;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.modality.nlp.translator.ZeroShotClassificationInput;
import ai.djl.modality.nlp.translator.ZeroShotClassificationOutput;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.inference.Predictor;
import ai.djl.translate.TranslateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class ContentFilterService {
    private static final Logger logger = LoggerFactory.getLogger(ContentFilterService.class);
    private final Predictor<ZeroShotClassificationInput, ZeroShotClassificationOutput> predictor;

    public ContentFilterService(ResourceLoader resourceLoader) throws Exception {

        // Load tokenizer using ResourceLoader
        var tokenizerPath = resourceLoader.getResource(
                "classpath:model/DeBERTa-v3-large-mnli-fever-anli-ling-wanli/tokenizer.json"
        ).getFile().toPath();

        // Load model path using ResourceLoader
        var modelPath = resourceLoader.getResource(
                "classpath:model/DeBERTa-v3-large-mnli-fever-anli-ling-wanli"
        ).getFile().toPath();

        // Load the tokenizer
        HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPath);

        // Create a custom translator
        CustomZeroShotClassificationTranslator translator = 
            CustomZeroShotClassificationTranslator.builder(tokenizer).build();

        // Set up the criteria for loading the model
        Criteria<ZeroShotClassificationInput, ZeroShotClassificationOutput> criteria = 
            Criteria.builder()
                .setTypes(ZeroShotClassificationInput.class, ZeroShotClassificationOutput.class)
                .optModelPath(modelPath)
                .optEngine("PyTorch")
                .optTranslator(translator)
                .build();

        // Load the model
        var model = ModelZoo.loadModel(criteria);
        this.predictor = model.newPredictor();
    }

    public boolean isPoliticsRelated(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        try {
            String[] candidateLabels = new String[] {"Politics"};
            ZeroShotClassificationInput input = new ZeroShotClassificationInput(text, candidateLabels, true);
            ZeroShotClassificationOutput output = predictor.predict(input);

            // Check if any score is above 0.90
            return Arrays.stream(output.getScores()).anyMatch(score -> score > 0.90);
        } catch (TranslateException e) {
            logger.error("Error classifying text: {}", e.getMessage(), e);
            return false;
        }
    }

    public void close() {
        predictor.close();
    }
}