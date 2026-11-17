package br.com.comparapreco.oferta.controller;

import java.net.URI;
import java.util.UUID;
import br.com.comparapreco.autenticacao.security.UsuarioSessao;
import br.com.comparapreco.oferta.dto.AtualizarOfertaRequest;
import br.com.comparapreco.oferta.dto.CriarOfertaRequest;
import br.com.comparapreco.oferta.dto.OfertaResponse;
import br.com.comparapreco.oferta.service.OfertaService;
import br.com.comparapreco.shared.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fornecedor/ofertas")
@PreAuthorize("hasRole('FORNECEDOR')")
public class OfertaController {
    private final OfertaService ofertas;
    public OfertaController(OfertaService ofertas) {
        this.ofertas = ofertas;
    }

    @PostMapping
    @Operation(security = @SecurityRequirement(name = "csrf"))
    public ResponseEntity<OfertaResponse> criar(@AuthenticationPrincipal UsuarioSessao usuario, @Valid @RequestBody CriarOfertaRequest request) {
        var oferta = ofertas.criar(usuario.id(), request);
        return ResponseEntity.created(URI.create("/api/fornecedor/ofertas/" + oferta.id())).body(oferta);
    }

    @GetMapping
    public PaginaResponse<OfertaResponse> listar(@AuthenticationPrincipal UsuarioSessao usuario,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        return ofertas.listarProprias(usuario.id(), pagina, tamanho);
    }

    @GetMapping("/{id}")
    public OfertaResponse consultar(@AuthenticationPrincipal UsuarioSessao usuario, @PathVariable UUID id) {
        return ofertas.consultarPropria(usuario.id(), id);
    }

    @PutMapping("/{id}")
    @Operation(security = @SecurityRequirement(name = "csrf"))
    public OfertaResponse atualizar(@AuthenticationPrincipal UsuarioSessao usuario, @PathVariable UUID id,
                                    @Valid @RequestBody AtualizarOfertaRequest request) {
        return ofertas.atualizar(usuario.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(security = @SecurityRequirement(name = "csrf"))
    public void excluir(@AuthenticationPrincipal UsuarioSessao usuario, @PathVariable UUID id,
                        @RequestParam @Min(0) long versao) {
        ofertas.excluir(usuario.id(), id, versao);
    }
}
