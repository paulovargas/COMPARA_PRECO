package br.com.comparapreco.fornecedor.controller;

import br.com.comparapreco.autenticacao.security.UsuarioSessao;
import br.com.comparapreco.fornecedor.dto.AtualizarFornecedorRequest;
import br.com.comparapreco.fornecedor.dto.FornecedorResponse;
import br.com.comparapreco.fornecedor.service.FornecedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fornecedor/perfil")
@PreAuthorize("hasRole('FORNECEDOR')")
public class FornecedorController {
    private final FornecedorService fornecedores;
    public FornecedorController(FornecedorService fornecedores) {
        this.fornecedores = fornecedores;
    }

    @GetMapping
    public FornecedorResponse consultar(@AuthenticationPrincipal UsuarioSessao usuario) {
        return fornecedores.consultarProprio(usuario.id());
    }

    @PutMapping
    @Operation(security = @SecurityRequirement(name = "csrf"))
    public FornecedorResponse atualizar(@AuthenticationPrincipal UsuarioSessao usuario, @Valid @RequestBody AtualizarFornecedorRequest request) {
        return fornecedores.atualizar(usuario.id(), request);
    }
}
