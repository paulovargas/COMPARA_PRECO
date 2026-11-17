package br.com.comparapreco.catalogo.controller;

import br.com.comparapreco.catalogo.dto.VarianteResponse;
import br.com.comparapreco.catalogo.service.CatalogoFornecedorService;
import br.com.comparapreco.shared.dto.PaginaResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fornecedor/catalogo/variantes")
@PreAuthorize("hasRole('FORNECEDOR')")
public class CatalogoFornecedorController {
    private final CatalogoFornecedorService catalogo;
    public CatalogoFornecedorController(CatalogoFornecedorService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping
    public PaginaResponse<VarianteResponse> consultar(@RequestParam(defaultValue = "") String texto,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        return catalogo.consultar(texto, pagina, tamanho);
    }
}
