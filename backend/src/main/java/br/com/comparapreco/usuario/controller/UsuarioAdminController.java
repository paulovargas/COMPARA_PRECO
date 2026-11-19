package br.com.comparapreco.usuario.controller;

import java.util.UUID;
import br.com.comparapreco.autenticacao.security.UsuarioSessao;
import br.com.comparapreco.shared.dto.PaginaResponse;
import br.com.comparapreco.usuario.dto.AcessoUsuarioRequest;
import br.com.comparapreco.usuario.dto.SituacaoUsuarioRequest;
import br.com.comparapreco.usuario.service.PermissoesService;
import br.com.comparapreco.usuario.dto.UsuarioResponse;
import br.com.comparapreco.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioAdminController {
    private final UsuarioService usuarios;
    private final PermissoesService permissoes;

    public UsuarioAdminController(UsuarioService usuarios, PermissoesService permissoes) {
        this.usuarios = usuarios;
        this.permissoes = permissoes;
    }

    @PutMapping("/{id}/acesso")
    @Operation(security = @SecurityRequirement(name = "csrf"))
    public UsuarioResponse alterarAcesso(@PathVariable UUID id, @Valid @RequestBody AcessoUsuarioRequest request,
            @AuthenticationPrincipal UsuarioSessao administrador) {
        return permissoes.alterar(id, request.ativo(), request.perfil(), request.versao(), administrador.id());
    }

    @GetMapping
    public PaginaResponse<UsuarioResponse> listar(@RequestParam(defaultValue = "0") int pagina,
                                                 @RequestParam(defaultValue = "20") int tamanho) {
        return usuarios.listar(pagina, tamanho);
    }

    @PatchMapping("/{id}/situacao")
    @Operation(security = @SecurityRequirement(name = "csrf"))
    public UsuarioResponse alterarSituacao(@PathVariable UUID id, @Valid @RequestBody SituacaoUsuarioRequest request,
                                           @AuthenticationPrincipal UsuarioSessao administrador) {
        return usuarios.alterarSituacao(id, request.ativo(), administrador.id());
    }
}
