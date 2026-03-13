package com.aimaster.service;

import com.aimaster.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithResponseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithResponseStreamResponseHandler;
import software.amazon.awssdk.services.bedrockruntime.model.PayloadPart;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class TextGenerationService {

    private final BedrockRuntimeClient bedrockClient;
    private final BedrockRuntimeAsyncClient bedrockAsyncClient;
    private final ModelCatalogService modelCatalog;
    private final StorageService storageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Markdown parser for formatting
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    public TextGenerationService(BedrockRuntimeClient bedrockClient,
                                  BedrockRuntimeAsyncClient bedrockAsyncClient,
                                  ModelCatalogService modelCatalog,
                                  StorageService storageService) {
        this.bedrockClient = bedrockClient;
        this.bedrockAsyncClient = bedrockAsyncClient;
        this.modelCatalog = modelCatalog;
        this.storageService = storageService;

        MutableDataSet options = new MutableDataSet();
        this.markdownParser = Parser.builder(options).build();
        this.htmlRenderer = HtmlRenderer.builder(options).build();
    }

    public Message generateResponse(ChatRequest request, Conversation conversation) {
        long startTime = System.currentTimeMillis();

        try {
            String modelId = request.getModelId();
            if (modelId == null || modelId.isEmpty()) {
                modelId = "us.anthropic.claude-sonnet-4-6";
            }
            final String finalModelId = modelId;

            ModelInfo model = modelCatalog.getModelById(modelId)
                    .orElseThrow(() -> new RuntimeException("Model not found: " + finalModelId));

            // Build the request body based on model provider
            String responseText;
            int inputTokens = 0;
            int outputTokens = 0;

            if (modelId.contains("anthropic") || modelId.contains("claude")) {
                var result = invokeClaudeModel(request, conversation, modelId);
                responseText = result[0];
                inputTokens = Integer.parseInt(result[1]);
                outputTokens = Integer.parseInt(result[2]);
            } else if (modelId.contains("amazon.nova") || modelId.contains("titan")) {
                var result = invokeNovaModel(request, conversation, modelId);
                responseText = result[0];
                inputTokens = Integer.parseInt(result[1]);
                outputTokens = Integer.parseInt(result[2]);
            } else if (modelId.contains("meta") || modelId.contains("llama")) {
                var result = invokeLlamaModel(request, conversation, modelId);
                responseText = result[0];
                inputTokens = Integer.parseInt(result[1]);
                outputTokens = Integer.parseInt(result[2]);
            } else if (modelId.contains("mistral")) {
                var result = invokeMistralModel(request, conversation, modelId);
                responseText = result[0];
                inputTokens = Integer.parseInt(result[1]);
                outputTokens = Integer.parseInt(result[2]);
            } else if (modelId.contains("cohere")) {
                var result = invokeCohereModel(request, conversation, modelId);
                responseText = result[0];
                inputTokens = Integer.parseInt(result[1]);
                outputTokens = Integer.parseInt(result[2]);
            } else {
                // Fallback generic
                var result = invokeClaudeModel(request, conversation, modelId);
                responseText = result[0];
                inputTokens = Integer.parseInt(result[1]);
                outputTokens = Integer.parseInt(result[2]);
            }

            long latency = System.currentTimeMillis() - startTime;

            // Convert markdown to HTML
            String htmlContent = markdownToHtml(responseText);

            Message assistantMsg = Message.builder()
                    .id(UUID.randomUUID().toString())
                    .role("assistant")
                    .content(responseText)
                    .formattedContent(htmlContent)
                    .timestamp(LocalDateTime.now())
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .latencyMs(latency)
                    .modelId(modelId)
                    .build();

            // Update conversation stats (conversation may be null for one-off calls like enhancePrompt)
            if (conversation != null) {
                conversation.setTotalTokensUsed(conversation.getTotalTokensUsed() + inputTokens + outputTokens);
                double cost = (inputTokens / 1000.0 * model.getInputCostPer1K())
                        + (outputTokens / 1000.0 * model.getOutputCostPer1K());
                conversation.setEstimatedCost(conversation.getEstimatedCost() + cost);
            }

            return assistantMsg;

        } catch (Exception e) {
            log.error("Error generating response", e);
            long latency = System.currentTimeMillis() - startTime;
            return Message.builder()
                    .id(UUID.randomUUID().toString())
                    .role("assistant")
                    .content("❌ Error: " + e.getMessage())
                    .formattedContent("<p class='text-danger'>❌ Error: " + e.getMessage() + "</p>")
                    .timestamp(LocalDateTime.now())
                    .latencyMs(latency)
                    .modelId(request.getModelId())
                    .build();
        }
    }

    private String[] invokeClaudeModel(ChatRequest request, Conversation conversation, String modelId) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("anthropic_version", "bedrock-2023-05-31");
        body.put("max_tokens", request.getMaxTokens() > 0 ? request.getMaxTokens() : 4096);
        // Claude API rejects requests with both temperature and top_p — send only temperature
        body.put("temperature", request.getTemperature() > 0 ? request.getTemperature() : 0.7);

        // System prompt
        String systemPrompt = request.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            systemPrompt = conversation != null ? conversation.getSystemPrompt() : null;
        }
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.put("system", systemPrompt);
        }

        // Build messages array
        ArrayNode messages = body.putArray("messages");

        // Add conversation history (last 20 messages)
        if (conversation != null && !conversation.getMessages().isEmpty()) {
            List<Message> history = conversation.getMessages();
            int start = Math.max(0, history.size() - 20);
            for (int i = start; i < history.size(); i++) {
                Message hist = history.get(i);
                if ("user".equals(hist.getRole()) || "assistant".equals(hist.getRole())) {
                    ObjectNode msgNode = messages.addObject();
                    msgNode.put("role", hist.getRole());
                    ArrayNode content = msgNode.putArray("content");
                    ObjectNode textBlock = content.addObject();
                    textBlock.put("type", "text");
                    textBlock.put("text", hist.getContent());
                }
            }
        }

        // Current user message
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        ArrayNode userContent = userMsg.putArray("content");

        // Add images if present
        if (request.getImagesBase64() != null) {
            for (int i = 0; i < request.getImagesBase64().size(); i++) {
                String b64 = request.getImagesBase64().get(i);
                String mime = (request.getImagesMimeTypes() != null && i < request.getImagesMimeTypes().size())
                        ? request.getImagesMimeTypes().get(i) : "image/jpeg";
                ObjectNode imageBlock = userContent.addObject();
                imageBlock.put("type", "image");
                ObjectNode source = imageBlock.putObject("source");
                source.put("type", "base64");
                source.put("media_type", mime);
                source.put("data", b64);
            }
        }

        // Add documents if present
        if (request.getDocumentsBase64() != null) {
            for (int i = 0; i < request.getDocumentsBase64().size(); i++) {
                String b64 = request.getDocumentsBase64().get(i);
                String name = (request.getDocumentsNames() != null && i < request.getDocumentsNames().size())
                        ? request.getDocumentsNames().get(i) : "document.pdf";
                ObjectNode docBlock = userContent.addObject();
                docBlock.put("type", "document");
                ObjectNode source = docBlock.putObject("source");
                source.put("type", "base64");
                source.put("media_type", "application/pdf");
                source.put("data", b64);
                docBlock.put("title", name);
            }
        }

        ObjectNode textBlock = userContent.addObject();
        textBlock.put("type", "text");
        textBlock.put("text", request.getMessage());

        String bodyString = objectMapper.writeValueAsString(body);

        InvokeModelRequest invokeRequest = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromString(bodyString, StandardCharsets.UTF_8))
                .contentType("application/json")
                .accept("application/json")
                .build();

        InvokeModelResponse response = bedrockClient.invokeModel(invokeRequest);
        String responseBody = response.body().asUtf8String();
        JsonNode responseNode = objectMapper.readTree(responseBody);

        String text = responseNode.path("content").get(0).path("text").asText();
        int inputTokens = responseNode.path("usage").path("input_tokens").asInt(0);
        int outputTokens = responseNode.path("usage").path("output_tokens").asInt(0);

        return new String[]{text, String.valueOf(inputTokens), String.valueOf(outputTokens)};
    }

    private String[] invokeNovaModel(ChatRequest request, Conversation conversation, String modelId) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();

        // System prompt for Nova
        String systemPrompt = request.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            systemPrompt = conversation != null ? conversation.getSystemPrompt() : null;
        }
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            ArrayNode systemArr = body.putArray("system");
            ObjectNode sysObj = systemArr.addObject();
            sysObj.put("text", systemPrompt);
        }

        ArrayNode messages = body.putArray("messages");

        // History
        if (conversation != null && !conversation.getMessages().isEmpty()) {
            List<Message> history = conversation.getMessages();
            int start = Math.max(0, history.size() - 20);
            for (int i = start; i < history.size(); i++) {
                Message hist = history.get(i);
                if ("user".equals(hist.getRole()) || "assistant".equals(hist.getRole())) {
                    ObjectNode msgNode = messages.addObject();
                    msgNode.put("role", hist.getRole());
                    ArrayNode content = msgNode.putArray("content");
                    ObjectNode textPart = content.addObject();
                    textPart.put("text", hist.getContent());
                }
            }
        }

        // Current message
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        ArrayNode userContent = userMsg.putArray("content");

        // Add images if present (Nova)
        if (request.getImagesBase64() != null) {
            for (int i = 0; i < request.getImagesBase64().size(); i++) {
                String b64 = request.getImagesBase64().get(i);
                String mime = (request.getImagesMimeTypes() != null && i < request.getImagesMimeTypes().size())
                        ? request.getImagesMimeTypes().get(i) : "image/jpeg";
                String fmt = mime.contains("/") ? mime.split("/")[1] : "jpeg";
                ObjectNode imgPart = userContent.addObject();
                ObjectNode image = imgPart.putObject("image");
                image.put("format", fmt);
                ObjectNode source = image.putObject("source");
                source.put("bytes", b64);
            }
        }

        ObjectNode textPart = userContent.addObject();
        textPart.put("text", request.getMessage());

        // Inference config
        ObjectNode inferenceConfig = body.putObject("inferenceConfig");
        inferenceConfig.put("maxTokens", request.getMaxTokens() > 0 ? request.getMaxTokens() : 4096);
        inferenceConfig.put("temperature", request.getTemperature() > 0 ? request.getTemperature() : 0.7);
        if (request.getTopP() > 0) inferenceConfig.put("topP", request.getTopP());

        String bodyString = objectMapper.writeValueAsString(body);

        InvokeModelRequest invokeRequest = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromString(bodyString, StandardCharsets.UTF_8))
                .contentType("application/json")
                .accept("application/json")
                .build();

        InvokeModelResponse response = bedrockClient.invokeModel(invokeRequest);
        JsonNode responseNode = objectMapper.readTree(response.body().asUtf8String());

        String text = responseNode.path("output").path("message").path("content").get(0).path("text").asText();
        int inputTokens = responseNode.path("usage").path("inputTokens").asInt(0);
        int outputTokens = responseNode.path("usage").path("outputTokens").asInt(0);

        return new String[]{text, String.valueOf(inputTokens), String.valueOf(outputTokens)};
    }

    private String[] invokeLlamaModel(ChatRequest request, Conversation conversation, String modelId) throws Exception {
        StringBuilder promptBuilder = new StringBuilder();

        String systemPrompt = request.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            systemPrompt = conversation != null ? conversation.getSystemPrompt() : "You are a helpful AI assistant.";
        }

        promptBuilder.append("<|begin_of_text|>");
        promptBuilder.append("<|start_header_id|>system<|end_header_id|>\n");
        promptBuilder.append(systemPrompt).append("<|eot_id|>");

        if (conversation != null && !conversation.getMessages().isEmpty()) {
            List<Message> history = conversation.getMessages();
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                Message hist = history.get(i);
                promptBuilder.append("<|start_header_id|>").append(hist.getRole()).append("<|end_header_id|>\n");
                promptBuilder.append(hist.getContent()).append("<|eot_id|>");
            }
        }

        promptBuilder.append("<|start_header_id|>user<|end_header_id|>\n");
        promptBuilder.append(request.getMessage()).append("<|eot_id|>");
        promptBuilder.append("<|start_header_id|>assistant<|end_header_id|>\n");

        ObjectNode body = objectMapper.createObjectNode();
        body.put("prompt", promptBuilder.toString());
        body.put("max_gen_len", request.getMaxTokens() > 0 ? request.getMaxTokens() : 2048);
        body.put("temperature", request.getTemperature() > 0 ? request.getTemperature() : 0.7);
        body.put("top_p", request.getTopP() > 0 ? request.getTopP() : 0.9);

        String bodyString = objectMapper.writeValueAsString(body);
        InvokeModelRequest invokeRequest = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromString(bodyString, StandardCharsets.UTF_8))
                .contentType("application/json")
                .accept("application/json")
                .build();

        InvokeModelResponse response = bedrockClient.invokeModel(invokeRequest);
        JsonNode responseNode = objectMapper.readTree(response.body().asUtf8String());

        String text = responseNode.path("generation").asText();
        int tokensGenerated = responseNode.path("generation_token_count").asInt(0);
        int promptTokens = responseNode.path("prompt_token_count").asInt(0);

        return new String[]{text, String.valueOf(promptTokens), String.valueOf(tokensGenerated)};
    }

    private String[] invokeMistralModel(ChatRequest request, Conversation conversation, String modelId) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        StringBuilder promptBuilder = new StringBuilder();

        if (conversation != null && !conversation.getMessages().isEmpty()) {
            List<Message> history = conversation.getMessages();
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                Message hist = history.get(i);
                if ("user".equals(hist.getRole())) {
                    promptBuilder.append("[INST] ").append(hist.getContent()).append(" [/INST]");
                } else {
                    promptBuilder.append(hist.getContent()).append("</s>");
                }
            }
        }
        promptBuilder.append("[INST] ").append(request.getMessage()).append(" [/INST]");

        body.put("prompt", promptBuilder.toString());
        body.put("max_tokens", request.getMaxTokens() > 0 ? request.getMaxTokens() : 2048);
        body.put("temperature", request.getTemperature() > 0 ? request.getTemperature() : 0.7);
        if (request.getTopP() > 0) body.put("top_p", request.getTopP());

        String bodyString = objectMapper.writeValueAsString(body);
        InvokeModelRequest invokeRequest = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromString(bodyString, StandardCharsets.UTF_8))
                .contentType("application/json")
                .accept("application/json")
                .build();

        InvokeModelResponse response = bedrockClient.invokeModel(invokeRequest);
        JsonNode responseNode = objectMapper.readTree(response.body().asUtf8String());

        String text = responseNode.path("outputs").get(0).path("text").asText();
        return new String[]{text, "0", "0"};
    }

    private String[] invokeCohereModel(ChatRequest request, Conversation conversation, String modelId) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("message", request.getMessage());
        body.put("max_tokens", request.getMaxTokens() > 0 ? request.getMaxTokens() : 2048);
        body.put("temperature", request.getTemperature() > 0 ? request.getTemperature() : 0.7);

        String systemPrompt = request.getSystemPrompt();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.put("preamble", systemPrompt);
        }

        if (conversation != null && !conversation.getMessages().isEmpty()) {
            ArrayNode chatHistory = body.putArray("chat_history");
            List<Message> history = conversation.getMessages();
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                Message hist = history.get(i);
                ObjectNode chatMsg = chatHistory.addObject();
                chatMsg.put("role", "user".equals(hist.getRole()) ? "USER" : "CHATBOT");
                chatMsg.put("message", hist.getContent());
            }
        }

        String bodyString = objectMapper.writeValueAsString(body);
        InvokeModelRequest invokeRequest = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromString(bodyString, StandardCharsets.UTF_8))
                .contentType("application/json")
                .accept("application/json")
                .build();

        InvokeModelResponse response = bedrockClient.invokeModel(invokeRequest);
        JsonNode responseNode = objectMapper.readTree(response.body().asUtf8String());

        String text = responseNode.path("text").asText();
        int inputTokens = responseNode.path("meta").path("billed_units").path("input_tokens").asInt(0);
        int outputTokens = responseNode.path("meta").path("billed_units").path("output_tokens").asInt(0);

        return new String[]{text, String.valueOf(inputTokens), String.valueOf(outputTokens)};
    }

    // ── Streaming response (SSE) ──────────────────────────────────────────────

    /**
     * Streams a Claude response via InvokeModelWithResponseStream.
     * Sends SSE events:
     *   data: {"type":"token","text":"..."}
     *   data: {"type":"done","conversationId":"...","inputTokens":N,"outputTokens":N}
     *   data: {"type":"error","message":"..."}
     *
     * For non-Claude models falls back to the synchronous path on a virtual thread.
     */
    public SseEmitter streamResponse(ChatRequest request, Conversation conversation) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout — kept alive by streaming

        String modelId = request.getModelId();
        if (modelId == null || modelId.isEmpty()) modelId = "us.anthropic.claude-sonnet-4-6";
        final String finalModelId = modelId;

        boolean isClaude = modelId.contains("anthropic") || modelId.contains("claude");

        if (isClaude) {
            // Build the same body as invokeClaudeModel but send via streaming API
            Thread.ofVirtual().start(() -> {
                try {
                    ObjectNode body = buildClaudeRequestBody(request, conversation, finalModelId);
                    String bodyString = objectMapper.writeValueAsString(body);

                    InvokeModelWithResponseStreamRequest streamReq =
                            InvokeModelWithResponseStreamRequest.builder()
                                    .modelId(finalModelId)
                                    .body(SdkBytes.fromString(bodyString, StandardCharsets.UTF_8))
                                    .contentType("application/json")
                                    .accept("application/json")
                                    .build();

                    StringBuilder fullText = new StringBuilder();
                    AtomicInteger inputTokens  = new AtomicInteger(0);
                    AtomicInteger outputTokens = new AtomicInteger(0);

                    InvokeModelWithResponseStreamResponseHandler handler =
                            InvokeModelWithResponseStreamResponseHandler.builder()
                                    .subscriber(InvokeModelWithResponseStreamResponseHandler.Visitor.builder()
                                            .onChunk(payloadPart -> {
                                                try {
                                                    String chunk = payloadPart.bytes().asUtf8String();
                                                    JsonNode node = objectMapper.readTree(chunk);
                                                    String type = node.path("type").asText("");

                                                    if ("content_block_delta".equals(type)) {
                                                        String delta = node.path("delta").path("text").asText("");
                                                        if (!delta.isEmpty()) {
                                                            fullText.append(delta);
                                                            String escaped = objectMapper.writeValueAsString(delta);
                                                            // writeValueAsString adds surrounding quotes — strip them
                                                            escaped = escaped.substring(1, escaped.length() - 1);
                                                            emitter.send(SseEmitter.event()
                                                                    .data("{\"type\":\"token\",\"text\":\"" + escaped + "\"}"));
                                                        }
                                                    } else if ("message_delta".equals(type)) {
                                                        outputTokens.set(node.path("usage").path("output_tokens").asInt(0));
                                                    } else if ("message_start".equals(type)) {
                                                        inputTokens.set(node.path("message").path("usage").path("input_tokens").asInt(0));
                                                    }
                                                } catch (Exception ex) {
                                                    log.warn("Error parsing stream chunk: {}", ex.getMessage());
                                                }
                                            })
                                            .build())
                                    .onComplete(() -> {
                                        try {
                                            // Save assistant message to conversation
                                            String htmlContent = markdownToHtml(fullText.toString());
                                            Message assistantMsg = Message.builder()
                                                    .id(UUID.randomUUID().toString())
                                                    .role("assistant")
                                                    .content(fullText.toString())
                                                    .formattedContent(htmlContent)
                                                    .timestamp(LocalDateTime.now())
                                                    .inputTokens(inputTokens.get())
                                                    .outputTokens(outputTokens.get())
                                                    .modelId(finalModelId)
                                                    .build();
                                            assistantMsg.setConversation(conversation);
                                            conversation.getMessages().add(assistantMsg);

                                            ModelInfo model = modelCatalog.getModelById(finalModelId).orElse(null);
                                            if (model != null) {
                                                conversation.setTotalTokensUsed(
                                                        conversation.getTotalTokensUsed() + inputTokens.get() + outputTokens.get());
                                                double cost = (inputTokens.get() / 1000.0 * model.getInputCostPer1K())
                                                        + (outputTokens.get() / 1000.0 * model.getOutputCostPer1K());
                                                conversation.setEstimatedCost(conversation.getEstimatedCost() + cost);
                                            }
                                            storageService.saveConversation(conversation);

                                            // Send done event with final server-rendered HTML + metadata
                                            String doneEvent = String.format(
                                                    "{\"type\":\"done\",\"conversationId\":\"%s\",\"conversationTitle\":%s,\"formattedContent\":%s,\"inputTokens\":%d,\"outputTokens\":%d,\"totalTokens\":%d,\"estimatedCost\":\"$%.6f\"}",
                                                    conversation.getId(),
                                                    objectMapper.writeValueAsString(conversation.getTitle()),
                                                    objectMapper.writeValueAsString(htmlContent),
                                                    inputTokens.get(), outputTokens.get(),
                                                    conversation.getTotalTokensUsed(),
                                                    conversation.getEstimatedCost());
                                            emitter.send(SseEmitter.event().data(doneEvent));
                                            emitter.complete();
                                        } catch (Exception ex) {
                                            log.error("Error completing stream", ex);
                                            emitter.completeWithError(ex);
                                        }
                                    })
                                    .onError(err -> {
                                        log.error("Bedrock stream error", err);
                                        try {
                                            emitter.send(SseEmitter.event()
                                                    .data("{\"type\":\"error\",\"message\":\"" + err.getMessage() + "\"}"));
                                        } catch (Exception ignored) {}
                                        emitter.completeWithError(err);
                                    })
                                    .build();

                    bedrockAsyncClient.invokeModelWithResponseStream(streamReq, handler).get();

                } catch (Exception ex) {
                    log.error("Streaming error", ex);
                    try {
                        emitter.send(SseEmitter.event()
                                .data("{\"type\":\"error\",\"message\":\"" + ex.getMessage() + "\"}"));
                    } catch (Exception ignored) {}
                    emitter.completeWithError(ex);
                }
            });
        } else {
            // Non-Claude: run sync call on virtual thread, emit full response as single token event
            Thread.ofVirtual().start(() -> {
                try {
                    Message msg = generateResponse(request, conversation);
                    msg.setConversation(conversation);
                    conversation.getMessages().add(msg);
                    storageService.saveConversation(conversation);

                    String doneEvent = String.format(
                            "{\"type\":\"done\",\"conversationId\":\"%s\",\"conversationTitle\":%s,\"formattedContent\":%s,\"inputTokens\":%d,\"outputTokens\":%d,\"totalTokens\":%d,\"estimatedCost\":\"$%.6f\"}",
                            conversation.getId(),
                            objectMapper.writeValueAsString(conversation.getTitle()),
                            objectMapper.writeValueAsString(msg.getFormattedContent()),
                            msg.getInputTokens(), msg.getOutputTokens(),
                            conversation.getTotalTokensUsed(),
                            conversation.getEstimatedCost());
                    emitter.send(SseEmitter.event().data(doneEvent));
                    emitter.complete();
                } catch (Exception ex) {
                    log.error("Fallback stream error", ex);
                    try {
                        emitter.send(SseEmitter.event()
                                .data("{\"type\":\"error\",\"message\":\"" + ex.getMessage() + "\"}"));
                    } catch (Exception ignored) {}
                    emitter.completeWithError(ex);
                }
            });
        }

        return emitter;
    }

    /** Extracts the Claude request body construction into a reusable method. */
    private ObjectNode buildClaudeRequestBody(ChatRequest request, Conversation conversation, String modelId) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("anthropic_version", "bedrock-2023-05-31");
        body.put("max_tokens", request.getMaxTokens() > 0 ? request.getMaxTokens() : 4096);
        body.put("temperature", request.getTemperature() > 0 ? request.getTemperature() : 0.7);

        String systemPrompt = request.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            systemPrompt = conversation != null ? conversation.getSystemPrompt() : null;
        }
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.put("system", systemPrompt);
        }

        ArrayNode messages = body.putArray("messages");
        if (conversation != null && !conversation.getMessages().isEmpty()) {
            List<Message> history = conversation.getMessages();
            int start = Math.max(0, history.size() - 20);
            for (int i = start; i < history.size(); i++) {
                Message hist = history.get(i);
                if ("user".equals(hist.getRole()) || "assistant".equals(hist.getRole())) {
                    ObjectNode msgNode = messages.addObject();
                    msgNode.put("role", hist.getRole());
                    ArrayNode content = msgNode.putArray("content");
                    ObjectNode textBlock = content.addObject();
                    textBlock.put("type", "text");
                    textBlock.put("text", hist.getContent());
                }
            }
        }

        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        ArrayNode userContent = userMsg.putArray("content");

        if (request.getImagesBase64() != null) {
            for (int i = 0; i < request.getImagesBase64().size(); i++) {
                String b64 = request.getImagesBase64().get(i);
                String mime = (request.getImagesMimeTypes() != null && i < request.getImagesMimeTypes().size())
                        ? request.getImagesMimeTypes().get(i) : "image/jpeg";
                ObjectNode imageBlock = userContent.addObject();
                imageBlock.put("type", "image");
                ObjectNode source = imageBlock.putObject("source");
                source.put("type", "base64");
                source.put("media_type", mime);
                source.put("data", b64);
            }
        }

        if (request.getDocumentsBase64() != null) {
            for (int i = 0; i < request.getDocumentsBase64().size(); i++) {
                String b64 = request.getDocumentsBase64().get(i);
                String name = (request.getDocumentsNames() != null && i < request.getDocumentsNames().size())
                        ? request.getDocumentsNames().get(i) : "document.pdf";
                ObjectNode docBlock = userContent.addObject();
                docBlock.put("type", "document");
                ObjectNode source = docBlock.putObject("source");
                source.put("type", "base64");
                source.put("media_type", "application/pdf");
                source.put("data", b64);
                docBlock.put("title", name);
            }
        }

        ObjectNode textBlock = userContent.addObject();
        textBlock.put("type", "text");
        textBlock.put("text", request.getMessage());

        return body;
    }

    public String enhancePrompt(String userPrompt, String purpose) {
        ChatRequest enhanceRequest = ChatRequest.builder()
                .modelId("us.anthropic.claude-sonnet-4-6")
                .systemPrompt("Você é um especialista em engenharia de prompts. Sua tarefa é aprimorar prompts do usuário para obter melhores resultados de modelos de IA. Retorne APENAS o prompt aprimorado, sem explicações, sem comentários, sem aspas. Escreva SEMPRE em português do Brasil.")
                .message("Aprimore este prompt de " + purpose + " para obter resultados melhores da IA. Retorne apenas o prompt melhorado, em português:\n\n" + userPrompt)
                .maxTokens(1024)
                .temperature(0.7)
                .build();
        Message msg = generateResponse(enhanceRequest, null);
        return msg.getContent();
    }

    public String markdownToHtml(String markdown) {
        if (markdown == null) return "";
        Node document = markdownParser.parse(markdown);
        return htmlRenderer.render(document);
    }
}
