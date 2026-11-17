package br.com.comparapreco.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Compara Preço API",
        version = "0.0.1",
        description = "Busca e comparação de ofertas entre fornecedores"))
public class OpenApiConfig {
}
