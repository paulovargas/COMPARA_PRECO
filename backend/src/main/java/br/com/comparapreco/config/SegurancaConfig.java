package br.com.comparapreco.config;

import java.util.List;
import java.util.Map;
import br.com.comparapreco.autenticacao.security.UsuarioAtivoFilter;
import br.com.comparapreco.shared.exception.ErroHttpWriter;
import br.com.comparapreco.usuario.entity.Usuario;
import br.com.comparapreco.usuario.repository.UsuarioRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
@EnableMethodSecurity
@EnableScheduling
public class SegurancaConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder("bcrypt", Map.of("bcrypt", new BCryptPasswordEncoder(12)));
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarios) {
        return email -> usuarios.findByEmail(Usuario.normalizarEmail(email)).map(usuario ->
                User.withUsername(usuario.getEmail()).password(usuario.getSenhaHash()).roles(usuario.getPerfil().name())
                        .disabled(!usuario.isAtivo()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public HttpSessionCsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(HttpSessionCsrfTokenRepository csrf) {
        return new CompositeSessionAuthenticationStrategy(List.of(new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrf)));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contexts,
            HttpSessionCsrfTokenRepository csrf, UsuarioRepository usuarios, ErroHttpWriter erros) throws Exception {
        return http
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .securityContext(context -> context.securityContextRepository(contexts))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .csrf(config -> config.csrfTokenRepository(csrf))
                .exceptionHandling(config -> config
                        .authenticationEntryPoint((request, response, exception) -> erros.escrever(response, 401,
                                "AUTENTICACAO_NECESSARIA", "Entre na sua conta para continuar."))
                        .accessDeniedHandler((request, response, exception) -> erros.escrever(response, 403,
                                "ACESSO_NEGADO", "Você não tem permissão para esta operação.")))
                .authorizeHttpRequests(config -> config
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/variantes", "/api/catalogo/variantes/*/ofertas",
                                "/api/catalogo/categorias", "/api/catalogo/marcas", "/api/catalogo/ofertas/*/contato").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/cadastro", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/fornecedores").hasAnyRole("COMPRADOR", "FORNECEDOR")
                        .requestMatchers("/api/admin/**", "/actuator/info").hasRole("ADMIN")
                        .requestMatchers("/api/fornecedor/**").hasRole("FORNECEDOR")
                        .requestMatchers("/api/auth/eu", "/api/conta/**").authenticated()
                        .anyRequest().denyAll())
                .logout(config -> config.logoutUrl("/api/auth/logout").deleteCookies("SESSION")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .addFilterBefore(new UsuarioAtivoFilter(usuarios, erros), AuthorizationFilter.class)
                .build();
    }
}
