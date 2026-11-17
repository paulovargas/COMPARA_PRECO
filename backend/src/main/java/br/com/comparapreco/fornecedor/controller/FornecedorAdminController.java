package br.com.comparapreco.fornecedor.controller;

import java.util.UUID;
import br.com.comparapreco.fornecedor.dto.FornecedorResponse;
import br.com.comparapreco.fornecedor.dto.ModeracaoFornecedorRequest;
import br.com.comparapreco.fornecedor.entity.SituacaoFornecedor;
import br.com.comparapreco.fornecedor.service.FornecedorService;
import br.com.comparapreco.shared.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/fornecedores")
@PreAuthorize("hasRole('ADMIN')")
public class FornecedorAdminController {
    private final FornecedorService fornecedores;
    public FornecedorAdminController(FornecedorService fornecedores) {
        this.fornecedores = fornecedores;
    }

    @GetMapping
    public PaginaResponse<FornecedorResponse> listar(@RequestParam(required = false) SituacaoFornecedor situacao,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        return fornecedores.listarAdministracao(situacao, pagina, tamanho);
    }

    @PatchMapping("/{id}/situacao")
    @Operation(security = @SecurityRequirement(name = "csrf"))
    public FornecedorResponse moderar(@PathVariable UUID id, @Valid @RequestBody ModeracaoFornecedorRequest request) {
        return fornecedores.moderar(id, request);
    }
}
