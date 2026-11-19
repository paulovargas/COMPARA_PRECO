package br.com.comparapreco.importacao;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import br.com.comparapreco.catalogo.entity.*;
import br.com.comparapreco.catalogo.repository.*;
import br.com.comparapreco.fornecedor.entity.*;
import br.com.comparapreco.fornecedor.repository.FornecedorRepository;
import br.com.comparapreco.oferta.entity.*;
import br.com.comparapreco.oferta.repository.OfertaRepository;
import br.com.comparapreco.support.ClienteApi;
import br.com.comparapreco.support.PostgresIntegrationTest;
import br.com.comparapreco.usuario.entity.*;
import br.com.comparapreco.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ImportacaoIntegrationTest extends PostgresIntegrationTest {
    private static final String URL = "/api/fornecedor/importacoes";
    private static final String SENHA = "Senha-importacao-testes-123!";
    @Value("${local.server.port}") private int port;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder encoder;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private FornecedorRepository empresas;
    @Autowired private OfertaRepository ofertas;
    @Autowired private CategoriaRepository categorias;
    @Autowired private MarcaRepository marcas;
    @Autowired private ProdutoRepository produtos;
    @Autowired private VarianteProdutoRepository variantes;
    private Usuario dono;
    private Fornecedor empresa;
    private VarianteProduto variante;
    private VarianteProduto outra;

    @BeforeEach
    void preparar() {
        limpar();
        dono = usuarios.saveAndFlush(new Usuario("Fornecedor teste", "importador@teste.local", encoder.encode(SENHA), Perfil.FORNECEDOR));
        empresa = new Fornecedor(dono, "Empresa Ltda", "Empresa teste", "Descrição", "5511999999999", "São Paulo", UnidadeFederativa.SP);
        empresa.moderar(SituacaoFornecedor.APROVADO, null);
        empresa = empresas.saveAndFlush(empresa);
        var categoria = categorias.saveAndFlush(new Categoria("Celulares", true, true, Set.of()));
        var marca = marcas.saveAndFlush(new Marca("Marca teste"));
        var produto = produtos.saveAndFlush(new Produto(categoria, marca, "Produto teste", "Modelo teste"));
        variante = variantes.saveAndFlush(new VarianteProduto(produto, 256, "preto", CondicaoProduto.NOVO, Map.of()));
        outra = variantes.saveAndFlush(new VarianteProduto(produto, 512, "preto", CondicaoProduto.NOVO, Map.of()));
    }
    @AfterEach
    void limpar() {
        jdbc.update("DELETE FROM compara_preco.oferta");
        jdbc.update("DELETE FROM compara_preco.fornecedor");
        jdbc.update("DELETE FROM compara_preco.spring_session");
        jdbc.update("DELETE FROM compara_preco.usuario");
        jdbc.update("DELETE FROM compara_preco.limite_acesso");
        jdbc.update("DELETE FROM compara_preco.variante_produto");
        jdbc.update("DELETE FROM compara_preco.produto");
        jdbc.update("DELETE FROM compara_preco.marca");
        jdbc.update("DELETE FROM compara_preco.categoria");
    }
    @Test
    void deveCriarSomenteAoConfirmarERepetirConfirmacaoSemDuplicar() throws Exception {
        try (var cliente = cliente()) {
            var previa = previa(cliente, linha(variante, "1234,56"));
            assertThat(previa.get("criar").asInt()).isEqualTo(1);
            assertThat(ofertas.count()).isZero();
            String path = URL + "/" + previa.get("id").asText();
            assertThat(cliente.get(path).statusCode()).isEqualTo(200);
            assertThat(cliente.enviar("POST", path + "/confirmacao", null).statusCode()).isEqualTo(200);
            assertThat(cliente.enviar("POST", path + "/confirmacao", null).statusCode()).isEqualTo(200);
            assertThat(ofertas.count()).isEqualTo(1);
            var oferta = ofertas.findAll().getFirst();
            assertThat(oferta.getPreco()).isEqualByComparingTo("1234.56");
            assertThat(oferta.getModeracao()).isEqualTo(ModeracaoOferta.PENDENTE);
            assertThat(previa(cliente, linha(variante, "1234.56")).get("ignorar").asInt()).isEqualTo(1);
        }
    }
    @Test
    void deveAtualizarPreservandoModeracaoEIgnorarValoresIguaisSemAlterarVersao() throws Exception {
        var oferta = new Oferta(empresa, variante, new BigDecimal("100.00"), 2, true);
        oferta.moderar(ModeracaoOferta.APROVADA, null);
        oferta = ofertas.saveAndFlush(oferta);
        try (var cliente = cliente()) {
            var previa = previa(cliente, linha(variante, "100"));
            confirmar(cliente, previa, 200);
            assertThat(ofertas.findById(oferta.getId()).orElseThrow().getVersao()).isEqualTo(oferta.getVersao());
            previa = previa(cliente, linha(variante, "200.10"));
            assertThat(previa.get("atualizar").asInt()).isEqualTo(1);
            confirmar(cliente, previa, 200);
            var atual = ofertas.findById(oferta.getId()).orElseThrow();
            assertThat(atual.getPreco()).isEqualByComparingTo("200.10");
            assertThat(atual.getModeracao()).isEqualTo(ModeracaoOferta.APROVADA);
        }
    }
    @Test
    void deveRecusarLoteInteiroComErrosEDuplicatas() throws Exception {
        try (var cliente = cliente()) {
            var previa = previa(cliente, linha(variante, "20") + linha(variante, "0") + UUID.randomUUID() + ";12.999;-1;sim\n");
            assertThat(previa.get("invalidas").asInt()).isEqualTo(3);
            assertThat(previa.get("linhas").get(0).get("erros").toString()).contains("repetida");
            confirmar(cliente, previa, 409);
            assertThat(ofertas.count()).isZero();
        }
    }
    @Test
    void deveRevalidarVersaoERollbackDeTodasAsLinhas() throws Exception {
        var oferta = ofertas.saveAndFlush(new Oferta(empresa, variante, new BigDecimal("100"), 2, true));
        try (var cliente = cliente()) {
            var previa = previa(cliente, linha(outra, "50") + linha(variante, "200"));
            oferta.atualizar(new BigDecimal("300"), 2, true); ofertas.saveAndFlush(oferta);
            confirmar(cliente, previa, 409);
            assertThat(ofertas.count()).isEqualTo(1);
            assertThat(ofertas.findById(oferta.getId()).orElseThrow().getPreco()).isEqualByComparingTo("300");
        }
    }
    @Test
    void deveDetectarOfertaCriadaDepoisDaPrevia() throws Exception {
        try (var cliente = cliente()) {
            var previa = previa(cliente, linha(variante, "200"));
            ofertas.saveAndFlush(new Oferta(empresa, variante, new BigDecimal("300"), 2, true));
            confirmar(cliente, previa, 409);
            assertThat(ofertas.findAll().getFirst().getPreco()).isEqualByComparingTo("300");
        }
    }
    @Test
    void deveIsolarPreviaEExigirCsrfEFornecedorAprovado() throws Exception {
        try (var cliente = cliente()) {
            assertThat(cliente.arquivo(URL, "ofertas.csv", csv(linha(variante, "10")), false).statusCode()).isEqualTo(403);
            var previa = previa(cliente, linha(variante, "200"));
            try (var visitante = new ClienteApi(port, mapper)) { assertThat(visitante.get(URL + "/" + previa.get("id").asText()).statusCode()).isEqualTo(401); }
            var outro = usuarios.saveAndFlush(new Usuario("Outro", "outro@teste.local", encoder.encode(SENHA), Perfil.FORNECEDOR));
            try (var clienteB = new ClienteApi(port, mapper)) {
                clienteB.login(outro.getEmail(), SENHA);
                assertThat(clienteB.get(URL + "/" + previa.get("id").asText()).statusCode()).isEqualTo(404);
            }
            empresa.moderar(SituacaoFornecedor.BLOQUEADO, "Revisão necessária"); empresas.saveAndFlush(empresa);
            confirmar(cliente, previa, 403);
            assertThat(cliente.arquivo(URL, "ofertas.csv", csv(linha(variante, "10")), true).statusCode()).isEqualTo(403);
        }
    }
    @Test
    void deveRecusarPreviaExpiradaELimitarPendencias() throws Exception {
        try (var cliente = cliente()) {
            var previa = previa(cliente, linha(variante, "10"));
            jdbc.update("UPDATE compara_preco.importacao_ofertas SET previa = replace(previa, ?, ?)", previa.get("expiraEm").asText(), Instant.now().minusSeconds(60).toString());
            confirmar(cliente, previa, 410);
            for (int i = 0; i < 9; i++) { previa(cliente, linha(variante, "10")); }
            assertThat(cliente.arquivo(URL, "ofertas.csv", csv(linha(variante, "10")), true).statusCode()).isEqualTo(409);
        }
    }
    @Test
    void deveImportarXlsxERecusarFormulas() throws Exception {
        try (var cliente = cliente(); var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var sheet = workbook.createSheet("Ofertas");
            var header = sheet.createRow(0);
            String[] nomes = {"varianteId", "preco", "estoque", "disponivel"};
            for (int i = 0; i < 4; i++) { header.createCell(i).setCellValue(nomes[i]); }
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue(variante.getId().toString()); row.createCell(1).setCellValue(20.50);
            row.createCell(2).setCellValue(2); row.createCell(3).setCellValue(true);
            var out = new java.io.ByteArrayOutputStream(); workbook.write(out);
            var resposta = cliente.arquivo(URL, "ofertas.xlsx", out.toByteArray(), true);
            assertThat(resposta.statusCode()).withFailMessage(resposta.body()).isEqualTo(200);
            confirmar(cliente, cliente.json(resposta), 200);
            row.getCell(1).setCellFormula("1+1"); out.reset(); workbook.write(out);
            assertThat(cliente.arquivo(URL, "ofertas.xlsx", out.toByteArray(), true).statusCode()).isEqualTo(400);
        }
    }
    @Test
    void deveValidarFormatoCabecalhoLimiteETamanho() throws Exception {
        try (var cliente = cliente()) {
            assertThat(cliente.arquivo(URL, "ofertas.xls", csv(linha(variante, "10")), true).statusCode()).isEqualTo(400);
            assertThat(cliente.arquivo(URL, "ofertas.csv", "incorreto\n".getBytes(StandardCharsets.UTF_8), true).statusCode()).isEqualTo(400);
            assertThat(cliente.arquivo(URL, "ofertas.csv", csv(linha(variante, "10").repeat(501)), true).statusCode()).isEqualTo(400);
            assertThat(cliente.arquivo(URL, "ofertas.csv", new byte[2 * 1024 * 1024 + 1], true).statusCode()).isEqualTo(413);
            assertThat(cliente.get(URL + "/modelo/csv").body()).startsWith("varianteId;preco;estoque;disponivel");
            assertThat(cliente.get(URL + "/modelo/xlsx").statusCode()).isEqualTo(200);
        }
    }
    @Test
    void deveSerializarConfirmacoesConcorrentesSemDuplicar() throws Exception {
        try (var cliente = cliente(); var outroCliente = cliente(); var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var previa = previa(cliente, linha(variante, "200"));
            String path = URL + "/" + previa.get("id").asText() + "/confirmacao";
            var inicio = new java.util.concurrent.CountDownLatch(1);
            var a = executor.submit(() -> { inicio.await(); return cliente.enviar("POST", path, null); });
            var b = executor.submit(() -> { inicio.await(); return outroCliente.enviar("POST", path, null); });
            inicio.countDown();
            assertThat(a.get(20, java.util.concurrent.TimeUnit.SECONDS).statusCode()).isEqualTo(200);
            assertThat(b.get(20, java.util.concurrent.TimeUnit.SECONDS).statusCode()).isEqualTo(200);
            assertThat(ofertas.count()).isEqualTo(1);
        }
    }
    @Test
    void deveRecusarVarianteIncompletaENaoConfundirIdentificadorAbreviado() throws Exception {
        var incompleta = variantes.saveAndFlush(new VarianteProduto(variante.getProduto(), null, "branco", CondicaoProduto.NOVO, Map.of()));
        try (var cliente = cliente()) {
            var previa = previa(cliente, linha(incompleta, "20") + "1-1-1-1-1;20;2;true\n");
            assertThat(previa.get("invalidas").asInt()).isEqualTo(2);
            assertThat(previa.get("linhas").get(0).get("erros").toString()).contains("incompleta");
            confirmar(cliente, previa, 409);
            assertThat(ofertas.count()).isZero();
        }
    }
    private ClienteApi cliente() throws Exception { var cliente = new ClienteApi(port, mapper); cliente.login(dono.getEmail(), SENHA); return cliente; }
    private String linha(VarianteProduto v, String preco) { return v.getId() + ";" + preco + ";2;true\n"; }
    private byte[] csv(String linhas) { return ("varianteId;preco;estoque;disponivel\n" + linhas).getBytes(StandardCharsets.UTF_8); }
    private JsonNode previa(ClienteApi cliente, String linhas) throws Exception {
        var resposta = cliente.arquivo(URL, "ofertas.csv", csv(linhas), true);
        assertThat(resposta.statusCode()).withFailMessage(resposta.body()).isEqualTo(200);
        return cliente.json(resposta);
    }
    private void confirmar(ClienteApi cliente, JsonNode previa, int status) throws Exception {
        var resposta = cliente.enviar("POST", URL + "/" + previa.get("id").asText() + "/confirmacao", null);
        assertThat(resposta.statusCode()).withFailMessage(resposta.body()).isEqualTo(status);
    }
}
