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
@Schema(description = "Resposta padrão de erro da API")
public class ApiErrorResponse {

    @Schema(description = "Código HTTP do erro", example = "400")
    private int status;

    @Schema(description = "Tipo do erro", example = "BAD_REQUEST")
    private String error;

    @Schema(description = "Mensagem descritiva do erro", example = "A mensagem é obrigatória")
    private String message;
}
