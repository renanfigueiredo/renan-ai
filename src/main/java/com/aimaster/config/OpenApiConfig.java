package com.aimaster.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI evjAiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EVJ AI API")
                        .version("1.0.0")
                        .description("""
                                API assíncrona de chat da plataforma EVJ AI.

                                ## Como usar

                                **1. Envie o prompt** (retorna instantaneamente com um `id`):

                                ```
                                POST /api/v1/chat  {"prompt": "sua pergunta"}
                                → {"id": "chat-abc123", "status": "PROCESSING"}
                                ```

                                **2. Consulte o resultado** (polling):

                                ```
                                GET /api/v1/chat/chat-abc123
                                → {"status": "COMPLETED", "result": "resposta da IA"}
                                ```
                                """)
                        .contact(new Contact().name("EVJ AI").url("https://evj.app.br")));
    }
}
