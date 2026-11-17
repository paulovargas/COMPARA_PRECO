package br.com.comparapreco.config;

import java.util.List;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Compara Preço API",
        version = "0.0.1",
        description = "Busca e comparação de ofertas entre fornecedores. Obtenha o token em /api/auth/csrf e informe-o em Authorize. Renove após login/logout."))
@SecurityScheme(name = "csrf", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER,
        paramName = "X-CSRF-TOKEN", description = "Token CSRF da sessão, obtido em GET /api/auth/csrf. Não é uma senha nem um token de login.")
public class OpenApiConfig {
    @Bean
    public OpenApiCustomizer documentarLogout() {
        return openApi -> openApi.path("/api/auth/logout", new PathItem().post(new Operation()
                .operationId("logout").summary("Encerra a sessão atual").tags(List.of("autenticacao-controller"))
                .addSecurityItem(new SecurityRequirement().addList("csrf"))
                .responses(new ApiResponses().addApiResponse("204", new ApiResponse().description("Sessão encerrada"))
                        .addApiResponse("403", new ApiResponse().description("Token CSRF ausente ou inválido")))));
    }
}
