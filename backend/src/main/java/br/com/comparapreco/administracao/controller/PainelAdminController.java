package br.com.comparapreco.administracao.controller;

import java.util.UUID;
import br.com.comparapreco.administracao.dto.*;
import br.com.comparapreco.administracao.dto.SolicitacaoAdminDtos.*;
import br.com.comparapreco.administracao.service.PainelAdminService;
import br.com.comparapreco.administracao.service.SolicitacaoAdminService;
import br.com.comparapreco.shared.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class PainelAdminController {
    private final PainelAdminService painel;
    private final SolicitacaoAdminService solicitacoes;
    public PainelAdminController(PainelAdminService painel, SolicitacaoAdminService solicitacoes) { this.painel = painel; this.solicitacoes = solicitacoes; }
    @GetMapping("/estatisticas") public EstatisticasResponse estatisticas() { return painel.estatisticas(); }
    @GetMapping("/relatorios/regioes.csv") public ResponseEntity<byte[]> relatorio() {
        return ResponseEntity.ok().header("Content-Type", "text/csv;charset=UTF-8").header("Content-Disposition", "attachment; filename=regioes.csv").body(painel.relatorio());
    }
    @GetMapping("/configuracoes") public ConfiguracaoDados configuracao() { return painel.configuracao(); }
    @PutMapping("/configuracoes")
    @Operation(security=@SecurityRequirement(name="csrf"))
    public ConfiguracaoDados configurar(@Valid @RequestBody ConfiguracaoDados r) { return painel.configurar(r); }
    @GetMapping("/solicitacoes")
    public PaginaResponse<SolicitacaoAdmin> solicitacoes(@RequestParam(required=false) String situacao, @RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="20") int tamanho) { return solicitacoes.listar(situacao, pagina, tamanho); }
    @PutMapping("/solicitacoes/{id}/resolucao")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @Operation(security=@SecurityRequirement(name="csrf"))
    public void resolver(@PathVariable UUID id, @Valid @RequestBody Resolucao r) { solicitacoes.resolver(id, r); }
}
