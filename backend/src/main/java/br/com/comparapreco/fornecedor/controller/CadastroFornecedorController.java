package br.com.comparapreco.fornecedor.controller;

import java.net.URI;
import br.com.comparapreco.autenticacao.security.UsuarioSessao;
import br.com.comparapreco.fornecedor.dto.DadosEmpresaRequest;
import br.com.comparapreco.fornecedor.dto.FornecedorResponse;
import br.com.comparapreco.fornecedor.service.FornecedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fornecedores")
@PreAuthorize("hasAnyRole('COMPRADOR','FORNECEDOR')")
public class CadastroFornecedorController {
    private final FornecedorService fornecedores;
    public CadastroFornecedorController(FornecedorService fornecedores) {
        this.fornecedores = fornecedores;
    }

    @PostMapping
    @Operation(security = @SecurityRequirement(name = "csrf"), description = "Cadastra empresa pendente e exige novo login com perfil FORNECEDOR.")
    public ResponseEntity<FornecedorResponse> cadastrar(@AuthenticationPrincipal UsuarioSessao usuario,
            @Valid @RequestBody DadosEmpresaRequest request, HttpServletRequest http) {
        var fornecedor = fornecedores.cadastrar(usuario.id(), request);
        var sessao = http.getSession(false);
        if (sessao != null) { sessao.invalidate(); }
        SecurityContextHolder.clearContext();
        return ResponseEntity.created(URI.create("/api/fornecedor/perfil")).body(fornecedor);
    }
}
