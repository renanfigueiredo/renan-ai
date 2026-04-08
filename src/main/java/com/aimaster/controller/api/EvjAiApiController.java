package com.aimaster.controller.api;

import com.aimaster.controller.api.dto.*;
import com.aimaster.model.ChatRequest;
import com.aimaster.service.TextGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "EVJ AI API")
public class EvjAiApiController {

    private final TextGenerationService textGenerationService;

    private final Map<String, ApiTaskResponse> tasks = new ConcurrentHashMap<>();

    // ── CHAT ──────────────────────────────────────────────────

    @PostMapping("/chat")
    @Operation(summary = "Enviar prompt de texto — retorna um ID para consultar o resultado")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Tarefa criada — consulte GET /api/v1/chat/{id}"),
            @ApiResponse(responseCode = "400", description = "Prompt inválido",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ApiTaskResponse> chat(@Valid @RequestBody ApiPromptRequest request) {
        var task = newTask("chat", request.getPrompt());

        var chatRequest = ChatRequest.builder()
                .message(request.getPrompt())
                .temperature(0.7)
                .maxTokens(4096)
                .topP(0.9)
                .build();

        // Uses the async streaming Bedrock client — same path as the internal chat, never times out
        textGenerationService.generateResponseAsync(chatRequest)
                .thenAccept(text -> {
                    if (text == null || text.isBlank()) {
                        fail(task, "Sem resposta da IA");
                    } else {
                        complete(task, text);
                    }
                })
                .exceptionally(ex -> {
                    fail(task, ex.getMessage());
                    return null;
                });

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(task);
    }

    @GetMapping("/chat/{id}")
    @Operation(summary = "Consultar resultado da geração de texto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status/resultado retornado"),
            @ApiResponse(responseCode = "404", description = "Tarefa não encontrada")
    })
    public ResponseEntity<ApiTaskResponse> getChatResult(
            @Parameter(description = "ID retornado no POST /chat", example = "chat-a1b2c3d4") @PathVariable String id) {
        return getTask(id);
    }

    // ── TASK HELPERS ──────────────────────────────────────────

    private ApiTaskResponse newTask(String type, String prompt) {
        var id = type + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        var task = ApiTaskResponse.builder()
                .id(id)
                .status("PROCESSING")
                .prompt(prompt)
                .createdAt(Instant.now().toString())
                .build();
        tasks.put(id, task);
        return task;
    }

    private void complete(ApiTaskResponse task, String result) {
        task.setStatus("COMPLETED");
        task.setResult(result);
        task.setCompletedAt(Instant.now().toString());
    }

    private void fail(ApiTaskResponse task, String error) {
        task.setStatus("FAILED");
        task.setError(error);
        task.setCompletedAt(Instant.now().toString());
    }

    private ResponseEntity<ApiTaskResponse> getTask(String id) {
        var task = tasks.get(id);
        return task != null ? ResponseEntity.ok(task) : ResponseEntity.notFound().build();
    }

    // ── EXCEPTION HANDLERS ────────────────────────────────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleError(RuntimeException ex) {
        log.error("API error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.builder()
                        .status(500).error("INTERNAL_ERROR").message(ex.getMessage()).build());
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        var msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("Requisição inválida");
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.builder()
                        .status(400).error("BAD_REQUEST").message(msg).build());
    }
}
