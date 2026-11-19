package br.com.comparapreco.importacao.controller;

import java.util.UUID;
import br.com.comparapreco.autenticacao.security.UsuarioSessao;
import br.com.comparapreco.importacao.dto.ImportacaoResponse;
import br.com.comparapreco.importacao.service.ImportacaoService;
import br.com.comparapreco.importacao.service.LeitorOfertas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/fornecedor/importacoes")
@PreAuthorize("hasRole('FORNECEDOR')")
public class ImportacaoController {
    private final ImportacaoService importacoes;
    private final LeitorOfertas leitor;
    public ImportacaoController(ImportacaoService importacoes, LeitorOfertas leitor) { this.importacoes = importacoes; this.leitor = leitor; }

    @GetMapping("/modelo/{formato}")
    public ResponseEntity<byte[]> modelo(@PathVariable String formato) {
        byte[] modelo = leitor.modelo(formato);
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=ofertas." + formato)
                .header("Content-Type", formato.equals("csv") ? "text/csv;charset=UTF-8" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").body(modelo);
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(security = @SecurityRequirement(name = "csrf"))
    public ImportacaoResponse preparar(@AuthenticationPrincipal UsuarioSessao usuario, @RequestParam MultipartFile arquivo) {
        return importacoes.preparar(usuario.id(), arquivo);
    }

    @GetMapping("/{id}")
    public ImportacaoResponse consultar(@AuthenticationPrincipal UsuarioSessao usuario, @PathVariable UUID id) {
        return importacoes.consultar(usuario.id(), id);
    }

    @PostMapping("/{id}/confirmacao")
    @Operation(security = @SecurityRequirement(name = "csrf"))
    public ImportacaoResponse confirmar(@AuthenticationPrincipal UsuarioSessao usuario, @PathVariable UUID id) {
        return importacoes.confirmar(usuario.id(), id);
    }
}
