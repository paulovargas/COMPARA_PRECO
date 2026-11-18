package br.com.comparapreco.catalogo.controller;

import br.com.comparapreco.autenticacao.security.UsuarioSessao;
import br.com.comparapreco.catalogo.dto.SolicitacaoCatalogoRequest;
import br.com.comparapreco.catalogo.dto.SolicitacaoCatalogoResponse;
import br.com.comparapreco.catalogo.service.SolicitacaoCatalogoService;
import br.com.comparapreco.shared.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fornecedor/catalogo/solicitacoes")
public class SolicitacaoCatalogoController {
    private final SolicitacaoCatalogoService service;
    public SolicitacaoCatalogoController(SolicitacaoCatalogoService service) { this.service = service; }
    @PostMapping
    @Operation(security=@SecurityRequirement(name="csrf"))
    public ResponseEntity<SolicitacaoCatalogoResponse> criar(@AuthenticationPrincipal UsuarioSessao usuario, @Valid @RequestBody SolicitacaoCatalogoRequest request) {
        return ResponseEntity.status(201).body(service.criar(usuario.id(), request));
    }
    @GetMapping
    public PaginaResponse<SolicitacaoCatalogoResponse> listar(@AuthenticationPrincipal UsuarioSessao usuario,
            @RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="20") int tamanho) {
        return service.listar(usuario.id(), pagina, tamanho);
    }
}
