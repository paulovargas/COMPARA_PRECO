package br.com.comparapreco.busca.controller;

import java.util.List;
import java.util.UUID;
import br.com.comparapreco.busca.dto.ComparacaoResponse;
import br.com.comparapreco.busca.dto.FiltroBusca;
import br.com.comparapreco.busca.dto.OpcaoCatalogoResponse;
import br.com.comparapreco.busca.dto.VarianteBuscaResponse;
import br.com.comparapreco.busca.service.BuscaService;
import br.com.comparapreco.shared.dto.PaginaResponse;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogo")
public class BuscaController {
    private final BuscaService service;
    public BuscaController(BuscaService service) { this.service = service; }

    @GetMapping("/variantes")
    public PaginaResponse<VarianteBuscaResponse> buscar(@Valid @ParameterObject @ModelAttribute FiltroBusca filtro) { return service.buscar(filtro); }

    @GetMapping("/variantes/{id}/ofertas")
    public ComparacaoResponse comparar(@PathVariable UUID id, @Valid @ParameterObject @ModelAttribute FiltroBusca filtro) { return service.comparar(id, filtro); }

    @GetMapping("/categorias")
    public List<OpcaoCatalogoResponse> categorias() { return service.categorias(); }

    @GetMapping("/marcas")
    public List<OpcaoCatalogoResponse> marcas() { return service.marcas(); }
}
