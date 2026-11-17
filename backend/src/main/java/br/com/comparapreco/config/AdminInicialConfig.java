package br.com.comparapreco.config;

import br.com.comparapreco.usuario.dto.CadastroRequest;
import br.com.comparapreco.usuario.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminInicialConfig {
    @Bean
    @ConditionalOnProperty(name = "app.admin-inicial.habilitado", havingValue = "true")
    public ApplicationRunner criarAdministradorInicial(UsuarioService usuarios,
            @Value("${app.admin-inicial.nome}") String nome, @Value("${app.admin-inicial.email}") String email,
            @Value("${app.admin-inicial.senha}") String senha) {
        return args -> usuarios.criarAdministradorInicial(new CadastroRequest(nome, email, senha));
    }
}
