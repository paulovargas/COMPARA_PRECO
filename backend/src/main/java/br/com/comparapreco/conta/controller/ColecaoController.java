package br.com.comparapreco.conta.controller;

import java.util.UUID;
import br.com.comparapreco.autenticacao.security.UsuarioSessao;
import br.com.comparapreco.conta.dto.FornecedorSeguidoResponse;
import br.com.comparapreco.conta.dto.MarcacaoResponse;
import br.com.comparapreco.conta.dto.ProdutoFavoritoResponse;
import br.com.comparapreco.conta.repository.ColecaoRepository.Tipo;
import br.com.comparapreco.conta.service.ColecaoService;
import br.com.comparapreco.shared.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conta")
public class ColecaoController {
    private final ColecaoService service;
    public ColecaoController(ColecaoService service) { this.service = service; }

    @GetMapping("/favoritos/produtos")
    public PaginaResponse<ProdutoFavoritoResponse> produtos(@AuthenticationPrincipal UsuarioSessao usuario,
            @RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="20") int tamanho) {
        return service.produtos(usuario.id(), pagina, tamanho);
    }
    @GetMapping("/fornecedores-seguidos")
    public PaginaResponse<FornecedorSeguidoResponse> fornecedores(@AuthenticationPrincipal UsuarioSessao usuario,
            @RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="20") int tamanho) {
        return service.fornecedores(usuario.id(), pagina, tamanho);
    }
    @GetMapping("/favoritos/produtos/{id}")
    public MarcacaoResponse produto(@AuthenticationPrincipal UsuarioSessao usuario, @PathVariable UUID id) {
        return new MarcacaoResponse(service.salvo(usuario.id(), id, Tipo.PRODUTO));
    }
    @GetMapping("/fornecedores-seguidos/{id}")
    public MarcacaoResponse fornecedor(@AuthenticationPrincipal UsuarioSessao usuario, @PathVariable UUID id) {
        return new MarcacaoResponse(service.salvo(usuario.id(), id, Tipo.FORNECEDOR));
    }
    @PutMapping("/favoritos/produtos/{id}")
    @Operation(security = @SecurityRequirement(name="csrf"))
    public ResponseEntity<Void> salvarProduto(@AuthenticationPrincipal UsuarioSessao usuario, @PathVariable UUID id) {
        service.salvar(usuario.id(), id, Tipo.PRODUTO); return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/favoritos/produtos/{id}")
    @Operation(security = @SecurityRequirement(name="csrf"))
    public ResponseEntity<Void> removerProduto(@AuthenticationPrincipal UsuarioSessao usuario, @PathVariable UUID id) {
        service.remover(usuario.id(), id, Tipo.PRODUTO); return ResponseEntity.noContent().build();
    }
    @PutMapping("/fornecedores-seguidos/{id}")
    @Operation(security = @SecurityRequirement(name="csrf"))
    public ResponseEntity<Void> seguir(@AuthenticationPrincipal UsuarioSessao usuario, @PathVariable UUID id) {
        service.salvar(usuario.id(), id, Tipo.FORNECEDOR); return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/fornecedores-seguidos/{id}")
    @Operation(security = @SecurityRequirement(name="csrf"))
    public ResponseEntity<Void> deixarSeguir(@AuthenticationPrincipal UsuarioSessao usuario, @PathVariable UUID id) {
        service.remover(usuario.id(), id, Tipo.FORNECEDOR); return ResponseEntity.noContent().build();
    }
}
