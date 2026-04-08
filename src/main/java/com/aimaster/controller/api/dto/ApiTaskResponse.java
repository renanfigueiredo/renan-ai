package com.aimaster.controller.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tarefa de geração — use o id para consultar o resultado via GET")
public class ApiTaskResponse {

    @Schema(description = "ID da tarefa — use para consultar o resultado", example = "chat-a1b2c3d4")
    private String id;

    @Schema(description = "Status: PROCESSING, COMPLETED ou FAILED", example = "PROCESSING")
    private String status;

    @Schema(description = "Prompt enviado", example = "Explique o Sermão do Monte")
    private String prompt;

    @Schema(description = "Resultado (disponível quando status = COMPLETED)")
    private String result;

    @Schema(description = "Mensagem de erro (quando status = FAILED)")
    private String error;

    @Schema(description = "Data/hora de criação", example = "2026-04-08T20:00:00Z")
    private String createdAt;

    @Schema(description = "Data/hora de conclusão", example = "2026-04-08T20:00:05Z")
    private String completedAt;
}
