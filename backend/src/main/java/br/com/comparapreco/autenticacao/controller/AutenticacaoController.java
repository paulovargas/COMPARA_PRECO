package br.com.comparapreco.autenticacao.controller;

import java.util.List;
import br.com.comparapreco.autenticacao.dto.CsrfResponse;
import br.com.comparapreco.autenticacao.dto.LoginRequest;
import br.com.comparapreco.autenticacao.security.UsuarioSessao;
import br.com.comparapreco.autenticacao.service.AutenticacaoService;
import br.com.comparapreco.autenticacao.service.LimiteAcessoService;
import br.com.comparapreco.usuario.dto.CadastroRequest;
import br.com.comparapreco.usuario.dto.UsuarioResponse;
import br.com.comparapreco.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {
    private final UsuarioService usuarios;
    private final AutenticacaoService autenticacao;
    private final SessionAuthenticationStrategy estrategia;
    private final SecurityContextRepository contexts;
    private final LimiteAcessoService limites;

    public AutenticacaoController(UsuarioService usuarios, AutenticacaoService autenticacao,
            SessionAuthenticationStrategy estrategia,
            SecurityContextRepository contexts, LimiteAcessoService limites) {
        this.usuarios = usuarios;
        this.autenticacao = autenticacao;
        this.estrategia = estrategia;
        this.contexts = contexts;
        this.limites = limites;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/cadastro")
    @Operation(security = @SecurityRequirement(name = "csrf"))
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroRequest request, HttpServletRequest http) {
        limites.verificarCadastro(http.getRemoteAddr());
        var usuario = usuarios.cadastrar(request);
        return ResponseEntity.status(201).body(usuario);
    }

    @PostMapping("/login")
    @Operation(security = @SecurityRequirement(name = "csrf"))
    public UsuarioResponse login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest http, HttpServletResponse response) {
        var resultado = autenticacao.autenticar(request, http.getRemoteAddr());
        var authentication = UsernamePasswordAuthenticationToken.authenticated(resultado.principal(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + resultado.principal().perfil().name())));
        estrategia.onAuthentication(authentication, http, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, http, response);
        return resultado.usuario();
    }

    @GetMapping("/eu")
    public UsuarioResponse atual(@AuthenticationPrincipal UsuarioSessao usuario) {
        return usuarios.consultar(usuario.id());
    }
}
