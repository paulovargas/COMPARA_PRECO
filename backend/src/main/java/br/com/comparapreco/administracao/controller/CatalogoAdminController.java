package br.com.comparapreco.administracao.controller;

import java.util.UUID;
import br.com.comparapreco.administracao.dto.CatalogoAdminDtos.*;
import br.com.comparapreco.administracao.service.CatalogoAdminService;
import br.com.comparapreco.shared.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/catalogo")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogoAdminController {
    private final CatalogoAdminService catalogo;
    public CatalogoAdminController(CatalogoAdminService catalogo) { this.catalogo = catalogo; }

    @GetMapping("/categorias")
    public PaginaResponse<CategoriaDados> categorias(@RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="20") int tamanho) { return catalogo.categorias(pagina, tamanho); }
    @PostMapping("/categorias")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @Operation(security = @SecurityRequirement(name="csrf"))
    public CategoriaDados criarCategoria(@Valid @RequestBody CategoriaDados r) { return catalogo.salvarCategoria(null, r); }
    @PutMapping("/categorias/{id}")
    @Operation(security = @SecurityRequirement(name="csrf"))
    public CategoriaDados atualizarCategoria(@PathVariable UUID id, @Valid @RequestBody CategoriaDados r) { return catalogo.salvarCategoria(id, r); }

    @GetMapping("/marcas")
    public PaginaResponse<MarcaDados> marcas(@RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="20") int tamanho) { return catalogo.marcas(pagina, tamanho); }
    @PostMapping("/marcas")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @Operation(security = @SecurityRequirement(name="csrf"))
    public MarcaDados criarMarca(@Valid @RequestBody MarcaDados r) { return catalogo.salvarMarca(null, r); }
    @PutMapping("/marcas/{id}")
    @Operation(security = @SecurityRequirement(name="csrf"))
    public MarcaDados atualizarMarca(@PathVariable UUID id, @Valid @RequestBody MarcaDados r) { return catalogo.salvarMarca(id, r); }

    @GetMapping("/produtos")
    public PaginaResponse<ProdutoDados> produtos(@RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="20") int tamanho) { return catalogo.produtos(pagina, tamanho); }
    @PostMapping("/produtos")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @Operation(security = @SecurityRequirement(name="csrf"))
    public ProdutoDados criarProduto(@Valid @RequestBody ProdutoDados r) { return catalogo.salvarProduto(null, r); }
    @PutMapping("/produtos/{id}")
    @Operation(security = @SecurityRequirement(name="csrf"))
    public ProdutoDados atualizarProduto(@PathVariable UUID id, @Valid @RequestBody ProdutoDados r) { return catalogo.salvarProduto(id, r); }

    @GetMapping("/variantes")
    public PaginaResponse<VarianteDados> variantes(@RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="20") int tamanho) { return catalogo.variantes(pagina, tamanho); }
    @PostMapping("/variantes")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @Operation(security = @SecurityRequirement(name="csrf"))
    public VarianteDados criarVariante(@Valid @RequestBody VarianteDados r) { return catalogo.salvarVariante(null, r); }
    @PutMapping("/variantes/{id}")
    @Operation(security = @SecurityRequirement(name="csrf"))
    public VarianteDados atualizarVariante(@PathVariable UUID id, @Valid @RequestBody VarianteDados r) { return catalogo.salvarVariante(id, r); }

    @DeleteMapping("/{tipo}/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @Operation(security = @SecurityRequirement(name="csrf"))
    public void excluir(@PathVariable String tipo, @PathVariable UUID id, @RequestParam @Min(0) long versao) { catalogo.excluir(tipo, id, versao); }
}
