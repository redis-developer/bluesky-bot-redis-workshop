package com.redis.filteringapp;

import ai.djl.Model;
import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.modality.nlp.translator.ZeroShotClassificationInput;
import ai.djl.modality.nlp.translator.ZeroShotClassificationOutput;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.*;
import ai.djl.translate.ArgumentsUtil;

import java.util.*;

public class CustomZeroShotClassificationTranslator implements NoBatchifyTranslator<ZeroShotClassificationInput, ZeroShotClassificationOutput> {

    private HuggingFaceTokenizer tokenizer;
    private boolean int32;
    private Predictor<NDList, NDList> predictor;

    private CustomZeroShotClassificationTranslator(HuggingFaceTokenizer tokenizer, boolean int32) {
        this.tokenizer = tokenizer;
        this.int32 = int32;
    }

    @Override
    public void prepare(TranslatorContext ctx) {
        Model model = ctx.getModel();
        this.predictor = model.newPredictor(new NoopTranslator(null));
        ctx.getPredictorManager().attachInternal(UUID.randomUUID().toString(), predictor);
    }

    @Override
    public NDList processInput(TranslatorContext ctx, ZeroShotClassificationInput input) {
        ctx.setAttachment("input", input);
        return new NDList();
    }

    @Override
    public ZeroShotClassificationOutput processOutput(TranslatorContext ctx, NDList list) throws TranslateException {
        ZeroShotClassificationInput input = (ZeroShotClassificationInput) ctx.getAttachment("input");

        String template = input.getHypothesisTemplate();
        List<String> candidates = List.of(input.getCandidates());
        if (candidates == null) {
            throw new TranslateException("Missing candidates in input");
        }

        NDManager manager = ctx.getNDManager();
        NDList output = new NDList(candidates.size());

        for (String candidate : candidates) {
            String hypothesis = applyTemplate(template, candidate);
            Encoding encoding = tokenizer.encode(input.getText(), hypothesis);
            NDList encoded = encoding.toNDList(manager, true, true);
            NDList batch = Batchifier.STACK.batchify(new NDList[]{encoded});
            output.add(predictor.predict(batch).get(0));
        }

        NDArray logits;
        if (input.isMultiLabel()) {
            int entailmentId = 0;
            int contradictionId = 2;
            NDList scores = new NDList();

            for (NDArray logits2 : output) {
                NDArray pair = logits2.get(":, {}", manager.create(new int[]{contradictionId, entailmentId}));
                NDArray probs = pair.softmax(1);
                NDArray entailmentScore = probs.get(":, 1");
                scores.add(entailmentScore);
            }

            logits = NDArrays.stack(scores).squeeze();
        } else {
            int entailmentId = 0;
            NDArray entailLogits = NDArrays.concat(output).get(":, " + entailmentId);
            NDArray exp = entailLogits.exp();
            NDArray sum = exp.sum();
            logits = exp.div(sum);
        }

        long[] indices = logits.argSort(-1, false).toLongArray();
        float[] probabilities = logits.toFloatArray();

        String[] labels = new String[candidates.size()];
        double[] scores = new double[candidates.size()];

        for (int i = 0; i < labels.length; i++) {
            int index = (int) indices[i];
            labels[i] = candidates.get(index);
            scores[i] = probabilities[index];
        }

        return new ZeroShotClassificationOutput(input.getText(), labels, scores);
    }

    private String applyTemplate(String template, String arg) {
        int pos = template.indexOf("{}");
        return pos == -1 ? template + arg : template.substring(0, pos) + arg + template.substring(pos + 2);
    }

    public static Builder builder(HuggingFaceTokenizer tokenizer) {
        return new Builder(tokenizer);
    }

    public static Builder builder(HuggingFaceTokenizer tokenizer, Map<String, ?> arguments) {
        return builder(tokenizer).configure(arguments);
    }

    public static class Builder {
        private HuggingFaceTokenizer tokenizer;
        private boolean int32 = false;

        public Builder(HuggingFaceTokenizer tokenizer) {
            this.tokenizer = tokenizer;
        }

        public Builder optInt32(boolean int32) {
            this.int32 = int32;
            return this;
        }

        public Builder configure(Map<String, ?> arguments) {
            this.int32 = ArgumentsUtil.booleanValue(arguments, "int32");
            return this;
        }

        public CustomZeroShotClassificationTranslator build() {
            return new CustomZeroShotClassificationTranslator(tokenizer, int32);
        }
    }
}
