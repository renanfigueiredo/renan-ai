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
@Schema(description = "Modelo de IA disponível")
public class ApiModelInfo {

    @Schema(description = "ID do modelo", example = "us.anthropic.claude-sonnet-4-6")
    private String id;

    @Schema(description = "Nome do modelo", example = "EVJ AI")
    private String name;

    @Schema(description = "Categoria: TEXT, IMAGE ou VIDEO", example = "TEXT")
    private String category;
}
