package br.com.comparapreco.administracao.service;

import java.util.UUID;
import br.com.comparapreco.administracao.dto.CatalogoAdminDtos.*;
import br.com.comparapreco.administracao.repository.ControleAdministrativoRepository;
import br.com.comparapreco.administracao.repository.CatalogoUsoRepository;
import br.com.comparapreco.catalogo.entity.*;
import br.com.comparapreco.catalogo.repository.*;
import br.com.comparapreco.shared.domain.ControleVersao;
import br.com.comparapreco.shared.domain.Paginacao;
import br.com.comparapreco.shared.dto.PaginaResponse;
import br.com.comparapreco.shared.exception.ErroNegocioException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class CatalogoAdminService {
    private final CategoriaRepository categorias;
    private final MarcaRepository marcas;
    private final ProdutoRepository produtos;
    private final VarianteProdutoRepository variantes;
    private final ControleAdministrativoRepository controle;
    private final CatalogoUsoRepository usos;
    public CatalogoAdminService(CategoriaRepository categorias, MarcaRepository marcas, ProdutoRepository produtos,
            VarianteProdutoRepository variantes, ControleAdministrativoRepository controle, CatalogoUsoRepository usos) {
        this.categorias = categorias; this.marcas = marcas; this.produtos = produtos; this.variantes = variantes; this.controle = controle; this.usos = usos;
    }
    @Transactional(readOnly=true)
    public PaginaResponse<CategoriaDados> categorias(int pagina, int tamanho) { return PaginaResponse.de(categorias.findAll(Paginacao.criar(pagina, tamanho, "criadoEm")).map(CatalogoAdminService::dados)); }
    @Transactional(readOnly=true)
    public PaginaResponse<MarcaDados> marcas(int pagina, int tamanho) { return PaginaResponse.de(marcas.findAll(Paginacao.criar(pagina, tamanho, "criadoEm")).map(CatalogoAdminService::dados)); }
    @Transactional(readOnly=true)
    public PaginaResponse<ProdutoDados> produtos(int pagina, int tamanho) { return PaginaResponse.de(produtos.findAll(Paginacao.criar(pagina, tamanho, "criadoEm")).map(CatalogoAdminService::dados)); }
    @Transactional(readOnly=true)
    public PaginaResponse<VarianteDados> variantes(int pagina, int tamanho) { return PaginaResponse.de(variantes.findAll(Paginacao.criar(pagina, tamanho, "criadoEm")).map(CatalogoAdminService::dados)); }

    @Transactional
    public CategoriaDados salvarCategoria(UUID id, CategoriaDados r) {
        controle.bloquearCatalogo();
        var validado = validar(() -> new Categoria(r.nome(), r.exigeArmazenamento(), r.exigeCor(), r.atributosObrigatorios()));
        if (id == null) { return dados(categorias.saveAndFlush(validado)); }
        var atual = categorias.findById(id).orElseThrow(CatalogoAdminService::ausente);
        ControleVersao.exigirAtual(r.versao(), atual.getVersao());
        if (usos.categoria(id) && (atual.isExigeArmazenamento() != validado.isExigeArmazenamento() || atual.isExigeCor() != validado.isExigeCor()
                || !atual.getAtributosObrigatorios().equals(validado.getAtributosObrigatorios()))) { throw emUso(); }
        atual.atualizar(validado.getNome(), validado.isExigeArmazenamento(), validado.isExigeCor(), validado.getAtributosObrigatorios());
        categorias.flush(); return dados(atual);
    }
    @Transactional
    public MarcaDados salvarMarca(UUID id, MarcaDados r) {
        controle.bloquearCatalogo(); var validado = validar(() -> new Marca(r.nome()));
        if (id == null) { return dados(marcas.saveAndFlush(validado)); }
        var atual = marcas.findById(id).orElseThrow(CatalogoAdminService::ausente); ControleVersao.exigirAtual(r.versao(), atual.getVersao());
        atual.atualizar(validado.getNome()); marcas.flush(); return dados(atual);
    }
    @Transactional
    public ProdutoDados salvarProduto(UUID id, ProdutoDados r) {
        controle.bloquearCatalogo();
        var categoria = categorias.findById(r.categoriaId()).orElseThrow(CatalogoAdminService::ausente);
        var marca = marcas.findById(r.marcaId()).orElseThrow(CatalogoAdminService::ausente);
        var validado = validar(() -> new Produto(categoria, marca, r.nome(), r.modelo(), vazio(r.descricao()), vazio(r.imagemUrl())));
        if (id == null) { return dados(produtos.saveAndFlush(validado)); }
        var atual = produtos.findById(id).orElseThrow(CatalogoAdminService::ausente); ControleVersao.exigirAtual(r.versao(), atual.getVersao());
        if (usos.produto(id) && (!atual.getCategoria().getId().equals(r.categoriaId()) || !atual.getMarca().getId().equals(r.marcaId())
                || !atual.getModelo().equals(validado.getModelo()))) { throw emUso(); }
        atual.atualizar(categoria, marca, validado.getNome(), validado.getModelo(), validado.getDescricao(), validado.getImagemUrl());
        produtos.flush(); return dados(atual);
    }
    @Transactional
    public VarianteDados salvarVariante(UUID id, VarianteDados r) {
        controle.bloquearCatalogo();
        var produto = produtos.findById(r.produtoId()).orElseThrow(CatalogoAdminService::ausente);
        var validado = validar(() -> new VarianteProduto(produto, r.armazenamentoGb(), vazio(r.cor()), r.condicao(), r.atributos()));
        if (id == null) { return dados(variantes.saveAndFlush(validado)); }
        var atual = variantes.buscarParaEdicao(id).orElseThrow(CatalogoAdminService::ausente); ControleVersao.exigirAtual(r.versao(), atual.getVersao());
        if (usos.variante(id)) { throw emUso(); }
        atual.atualizar(produto, validado.getArmazenamentoGb(), validado.getCor(), validado.getCondicao(), validado.getAtributos());
        variantes.flush(); return dados(atual);
    }
    @Transactional
    public void excluir(String tipo, UUID id, long versao) {
        controle.bloquearCatalogo();
        switch (tipo) {
            case "categorias" -> { var item = categorias.findById(id).orElseThrow(CatalogoAdminService::ausente); ControleVersao.exigirAtual(versao, item.getVersao()); categorias.delete(item); categorias.flush(); }
            case "marcas" -> { var item = marcas.findById(id).orElseThrow(CatalogoAdminService::ausente); ControleVersao.exigirAtual(versao, item.getVersao()); marcas.delete(item); marcas.flush(); }
            case "produtos" -> { var item = produtos.findById(id).orElseThrow(CatalogoAdminService::ausente); ControleVersao.exigirAtual(versao, item.getVersao()); produtos.delete(item); produtos.flush(); }
            case "variantes" -> { var item = variantes.buscarParaEdicao(id).orElseThrow(CatalogoAdminService::ausente); ControleVersao.exigirAtual(versao, item.getVersao()); variantes.delete(item); variantes.flush(); }
            default -> throw ausente();
        }
    }
    private static <T> T validar(java.util.function.Supplier<T> construir) {
        try { return construir.get(); } catch (IllegalArgumentException e) { throw new ErroNegocioException(400, "CATALOGO_INVALIDO", "Confira textos, características e URL da imagem."); }
    }
    private static String vazio(String s) { return s == null || s.isBlank() ? null : s.strip(); }
    private static ErroNegocioException ausente() { return new ErroNegocioException(404, "CATALOGO_NAO_ENCONTRADO", "Registro de catálogo não encontrado."); }
    private static ErroNegocioException emUso() { return new ErroNegocioException(409, "CATALOGO_EM_USO", "As características deste registro já estão em uso. Cadastre uma nova variante/produto em vez de mudar sua identidade."); }
    private static CategoriaDados dados(Categoria c) { return new CategoriaDados(c.getId(), c.getNome(), c.isExigeArmazenamento(), c.isExigeCor(), c.getAtributosObrigatorios(), c.getVersao()); }
    private static MarcaDados dados(Marca m) { return new MarcaDados(m.getId(), m.getNome(), m.getVersao()); }
    private static ProdutoDados dados(Produto p) { return new ProdutoDados(p.getId(), p.getCategoria().getId(), p.getMarca().getId(), p.getNome(), p.getModelo(), p.getDescricao(), p.getImagemUrl(), p.getVersao()); }
    private static VarianteDados dados(VarianteProduto v) { return new VarianteDados(v.getId(), v.getProduto().getId(), v.getArmazenamentoGb(), v.getCor(), v.getCondicao(), v.getAtributos(), v.getVersao(), v.getProduto().getNome(), v.getProduto().getModelo(), v.getProduto().getMarca().getNome()); }
}
