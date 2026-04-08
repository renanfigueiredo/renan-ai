package com.aimaster.controller.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requisição — basta enviar o prompt")
public class ApiPromptRequest {

    @NotBlank(message = "O prompt é obrigatório")
    @Size(max = 50000, message = "O prompt deve ter no máximo 50.000 caracteres")
    @Schema(description = "Texto do prompt",
            example = "Explique o Sermão do Monte em Mateus 5",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String prompt;
}
