package br.com.comparapreco.catalogo.repository;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import br.com.comparapreco.catalogo.entity.Categoria;
import br.com.comparapreco.catalogo.entity.CondicaoProduto;
import br.com.comparapreco.catalogo.entity.Marca;
import br.com.comparapreco.catalogo.entity.Produto;
import br.com.comparapreco.catalogo.entity.VarianteProduto;
import br.com.comparapreco.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CatalogoPersistenceTest extends PostgresIntegrationTest {

    @Autowired private CategoriaRepository categorias;
    @Autowired private MarcaRepository marcas;
    @Autowired private ProdutoRepository produtos;
    @Autowired private VarianteProdutoRepository variantes;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void devePersistirRelacionamentosEAtributosJsonb() {
        Produto produto = criarProduto();
        var variante = variantes.saveAndFlush(new VarianteProduto(produto, 256, "  PRETO ",
                CondicaoProduto.NOVO, Map.of("RAM_GB", "8", "acabamento", " Fosco ")));
        entityManager.clear();

        var salva = variantes.findById(variante.getId()).orElseThrow();
        assertThat(salva.getProduto().getMarca().getNome()).isEqualTo("Marca de teste");
        assertThat(salva.getProduto().getCategoria().getNome()).isEqualTo("Categoria de teste");
        assertThat(salva.getCor()).isEqualTo("preto");
        assertThat(salva.getAtributos()).containsEntry("ram_gb", "8").containsEntry("acabamento", "fosco");
        assertThat(salva.getCriadoEm()).isNotNull();
    }

    @Test
    void deveRejeitarCategoriasEquivalentes() {
        categorias.saveAndFlush(new Categoria("Smartphones"));
        assertThatThrownBy(() -> categorias.saveAndFlush(new Categoria("  SMARTPHONES  ")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarMarcasEquivalentes() {
        marcas.saveAndFlush(new Marca("Marca de teste"));
        assertThatThrownBy(() -> marcas.saveAndFlush(new Marca("MARCA   DE TESTE")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarMesmoModeloDaMarcaMesmoComOutroNomeOuCategoria() {
        Produto produto = criarProduto();
        Categoria outraCategoria = categorias.saveAndFlush(new Categoria("Outra categoria"));
        assertThatThrownBy(() -> produtos.saveAndFlush(new Produto(outraCategoria, produto.getMarca(),
                "Título diferente", " MODELO   DE TESTE "))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarVariantesEquivalentesIndependentementeDaOrdemDosAtributos() {
        Produto produto = criarProduto();
        variantes.saveAndFlush(new VarianteProduto(produto, 256, "preto", CondicaoProduto.NOVO,
                Map.of("ram_gb", "8", "acabamento", "fosco")));
        assertThatThrownBy(() -> variantes.saveAndFlush(new VarianteProduto(produto, 256, " PRETO ",
                CondicaoProduto.NOVO, Map.of("ACABAMENTO", "FOSCO", "RAM_GB", " 8 "))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarDuplicatasTambemQuandoCamposOpcionaisForemNulos() {
        Produto produto = criarProduto();
        variantes.saveAndFlush(new VarianteProduto(produto, null, null, CondicaoProduto.NOVO, Map.of()));
        assertThatThrownBy(() -> variantes.saveAndFlush(
                new VarianteProduto(produto, null, null, CondicaoProduto.NOVO, Map.of())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveManterVariantesDiferentesSeparadas() {
        Produto produto = criarProduto();
        Produto outroProduto = produtos.saveAndFlush(new Produto(produto.getCategoria(), produto.getMarca(),
                "Outro produto", "Outro modelo"));
        var diferentes = Stream.of(
                new VarianteProduto(produto, 256, "preto", CondicaoProduto.NOVO, Map.of("ram_gb", "8")),
                new VarianteProduto(produto, 512, "preto", CondicaoProduto.NOVO, Map.of("ram_gb", "8")),
                new VarianteProduto(produto, 256, "branco", CondicaoProduto.NOVO, Map.of("ram_gb", "8")),
                new VarianteProduto(produto, 256, "preto", CondicaoProduto.USADO, Map.of("ram_gb", "8")),
                new VarianteProduto(produto, 256, "preto", CondicaoProduto.NOVO, Map.of("ram_gb", "16")),
                new VarianteProduto(produto, null, null, CondicaoProduto.RECONDICIONADO, Map.of()),
                new VarianteProduto(outroProduto, 256, "preto", CondicaoProduto.NOVO, Map.of("ram_gb", "8"))).toList();
        variantes.saveAllAndFlush(diferentes);
        entityManager.clear();
        assertThat(variantes.findAll()).hasSize(7);
    }

    @ParameterizedTest
    @MethodSource("variantesInvalidas")
    void deveRejeitarDadosInvalidosInseridosDiretamenteNoBanco(Integer armazenamento, String cor,
                                                            String condicao, String atributos) {
        Produto produto = criarProduto();
        assertThatThrownBy(() -> inserirVariante(produto.getId(), armazenamento, cor, condicao, atributos))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    static Stream<Arguments> variantesInvalidas() {
        return Stream.of(
                Arguments.of(0, "preto", "NOVO", "{}"),
                Arguments.of(-1, "preto", "NOVO", "{}"),
                Arguments.of(256, "PRETO", "NOVO", "{}"),
                Arguments.of(256, "preto", "DESCONHECIDO", "{}"),
                Arguments.of(256, "preto", "NOVO", "[]"),
                Arguments.of(256, "preto", "NOVO", "{\"ram_gb\":8}"),
                Arguments.of(256, "preto", "NOVO", "{\"cor\":\"preto\"}"),
                Arguments.of(256, "preto", "NOVO", "{\"RAM_GB\":\"8\"}"));
    }

    @Test
    void deveImpedirVarianteSemProdutoExistente() {
        assertThatThrownBy(() -> inserirVariante(UUID.randomUUID(), 256, "preto", "NOVO", "{}"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveImpedirExclusaoDeMarcaComProdutoReferenciado() {
        Produto produto = criarProduto();
        assertThatThrownBy(() -> jdbc.update("DELETE FROM compara_preco.marca WHERE id = ?", produto.getMarca().getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveManterBancoPadraoSemCargaDemo() {
        assertThat(produtos.count()).isZero();
        assertThat(categorias.count()).isZero();
    }

    @Test
    void deveCarregarDadosDemoDeFormaIdempotente() {
        executarCargaDemo();
        executarCargaDemo();
        entityManager.clear();
        assertThat(categorias.count()).isEqualTo(2);
        assertThat(marcas.count()).isEqualTo(2);
        assertThat(produtos.count()).isEqualTo(3);
        assertThat(variantes.count()).isEqualTo(6);
        var variante = variantes.findById(UUID.fromString("40000000-0000-0000-0000-000000000001")).orElseThrow();
        assertThat(variante.getArmazenamentoGb()).isEqualTo(256);
        assertThat(variante.getAtributos()).containsEntry("ram_gb", "8");
    }

    private Produto criarProduto() {
        Categoria categoria = categorias.saveAndFlush(new Categoria("Categoria de teste"));
        Marca marca = marcas.saveAndFlush(new Marca("Marca de teste"));
        return produtos.saveAndFlush(new Produto(categoria, marca, "Produto de teste", "Modelo de teste"));
    }

    private void inserirVariante(UUID produto, Integer armazenamento, String cor, String condicao, String atributos) {
        jdbc.update("INSERT INTO compara_preco.variante_produto "
                + "(id, produto_id, armazenamento_gb, cor, condicao, atributos) VALUES (?, ?, ?, ?, ?, ?::jsonb)",
                UUID.randomUUID(), produto, armazenamento, cor, condicao, atributos);
    }

    private void executarCargaDemo() {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/demo/R__catalogo_demo.sql"));
            return null;
        });
    }
}
