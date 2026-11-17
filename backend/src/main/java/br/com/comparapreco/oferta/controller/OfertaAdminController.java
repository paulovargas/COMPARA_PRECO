package br.com.comparapreco.oferta.controller;

import java.util.UUID;
import br.com.comparapreco.oferta.dto.ModeracaoOfertaRequest;
import br.com.comparapreco.oferta.dto.OfertaResponse;
import br.com.comparapreco.oferta.entity.ModeracaoOferta;
import br.com.comparapreco.oferta.service.OfertaService;
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
@RequestMapping("/api/admin/ofertas")
@PreAuthorize("hasRole('ADMIN')")
public class OfertaAdminController {
    private final OfertaService ofertas;
    public OfertaAdminController(OfertaService ofertas) {
        this.ofertas = ofertas;
    }

    @GetMapping
    public PaginaResponse<OfertaResponse> listar(@RequestParam(required = false) ModeracaoOferta moderacao,
            @RequestParam(required = false) UUID fornecedorId, @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ofertas.listarAdministracao(moderacao, fornecedorId, pagina, tamanho);
    }

    @PatchMapping("/{id}/moderacao")
    @Operation(security = @SecurityRequirement(name = "csrf"))
    public OfertaResponse moderar(@PathVariable UUID id, @Valid @RequestBody ModeracaoOfertaRequest request) {
        return ofertas.moderar(id, request);
    }
}
