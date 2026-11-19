package br.com.comparapreco.administracao;

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
class AdministracaoIntegrationTest extends PostgresIntegrationTest {
    private static final String URL = "/api/admin/catalogo";
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
    private Usuario admin;
    private Usuario dono;
    private Fornecedor empresa;
    private VarianteProduto variante;
    private VarianteProduto outra;

    @BeforeEach
    void preparar() {
        limpar();
        dono = usuarios.saveAndFlush(new Usuario("Fornecedor teste", "importador@teste.local", encoder.encode(SENHA), Perfil.FORNECEDOR));
        admin = usuarios.saveAndFlush(new Usuario("Administrador teste", "admin@teste.local", encoder.encode(SENHA), Perfil.ADMIN));
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
        jdbc.update("UPDATE compara_preco.configuracao_sistema SET abertura_contato='Olá! Encontrei sua oferta no Compara Preço e tenho interesse em', versao=0");
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
    void deveExigirAdministradorECsrfEmTodasAsAreas() throws Exception {
        String[] urls = {"/api/admin/usuarios", "/api/admin/fornecedores", "/api/admin/ofertas", "/api/admin/catalogo/categorias", "/api/admin/catalogo/marcas", "/api/admin/catalogo/produtos", "/api/admin/catalogo/variantes", "/api/admin/solicitacoes", "/api/admin/estatisticas", "/api/admin/configuracoes", "/api/admin/relatorios/regioes.csv"};
        try (var visitante = new ClienteApi(port, mapper); var fornecedor = cliente(dono); var administrador = cliente(admin)) {
            for (String url : urls) { assertThat(visitante.get(url).statusCode()).isEqualTo(401); assertThat(fornecedor.get(url).statusCode()).isEqualTo(403); assertThat(administrador.get(url).statusCode()).withFailMessage(url).isEqualTo(200); }
            assertThat(administrador.enviarSemCsrf("POST", URL + "/marcas", Map.of("nome", "Marca nova", "versao", 0)).statusCode()).isEqualTo(403);
        }
    }
    @Test
    void deveCriarEditarExcluirMarcaComVersaoEDuplicidade() throws Exception {
        try (var cliente = cliente(admin)) {
            var criada = json(cliente, cliente.enviar("POST", URL + "/marcas", Map.of("nome", "Marca nova", "versao", 0)), 201);
            String path = URL + "/marcas/" + criada.get("id").asText();
            assertThat(cliente.enviar("POST", URL + "/marcas", Map.of("nome", "Marca nova", "versao", 0)).statusCode()).isEqualTo(409);
            var atual = json(cliente, cliente.enviar("PUT", path, Map.of("nome", "Marca revisada", "versao", 0)), 200);
            assertThat(atual.get("versao").asLong()).isEqualTo(1);
            assertThat(cliente.enviar("PUT", path, Map.of("nome", "Marca antiga", "versao", 0)).statusCode()).isEqualTo(409);
            assertThat(cliente.enviar("DELETE", path + "?versao=0", null).statusCode()).isEqualTo(409);
            assertThat(cliente.enviar("DELETE", path + "?versao=1", null).statusCode()).isEqualTo(204);
        }
    }
    @Test
    void devePreservarRegrasDaCategoriaEIdentidadeDoProdutoEmUso() throws Exception {
        var produto = variante.getProduto(); var categoria = produto.getCategoria();
        try (var cliente = cliente(admin)) {
            assertThat(cliente.enviar("PUT", URL + "/categorias/" + categoria.getId(), Map.of("nome", categoria.getNome(), "exigeArmazenamento", false, "exigeCor", false, "atributosObrigatorios", Set.of(), "versao", 0)).statusCode()).isEqualTo(409);
            assertThat(cliente.enviar("PUT", URL + "/categorias/" + categoria.getId(), Map.of("nome", "Categoria revisada", "exigeArmazenamento", true, "exigeCor", true, "atributosObrigatorios", Set.of(), "versao", 0)).statusCode()).isEqualTo(200);
            assertThat(cliente.enviar("DELETE", URL + "/categorias/" + categoria.getId() + "?versao=1", null).statusCode()).isEqualTo(409);
            var dados = Map.of("nome", "Nome revisado", "categoriaId", categoria.getId(), "marcaId", produto.getMarca().getId(), "modelo", "Outro modelo", "versao", 0);
            assertThat(cliente.enviar("PUT", URL + "/produtos/" + produto.getId(), dados).statusCode()).isEqualTo(409);
        }
    }
    @Test
    void deveCriarProdutoComImagemEVarianteCompletaSemAgruparIdentidades() throws Exception {
        try (var cliente = cliente(admin)) {
            var produto = variante.getProduto();
            var novo = json(cliente, cliente.enviar("POST", URL + "/produtos", Map.of("nome", "Outro produto", "modelo", "Modelo novo", "categoriaId", produto.getCategoria().getId(), "marcaId", produto.getMarca().getId(), "imagemUrl", "https://example.test/imagem.png", "versao", 0)), 201);
            var criada = json(cliente, cliente.enviar("POST", URL + "/variantes", Map.of("produtoId", novo.get("id").asText(), "armazenamentoGb", 128, "cor", "AZUL", "condicao", "NOVO", "atributos", Map.of("ram_gb", "8"), "versao", 0)), 201);
            assertThat(criada.get("cor").asText()).isEqualTo("azul");
            assertThat(cliente.enviar("POST", URL + "/variantes", Map.of("produtoId", novo.get("id").asText(), "armazenamentoGb", 128, "cor", "azul", "condicao", "NOVO", "atributos", Map.of("ram_gb", "8"), "versao", 0)).statusCode()).isEqualTo(409);
            assertThat(cliente.enviar("POST", URL + "/produtos", Map.of("nome", "Inválido", "modelo", "Outro", "categoriaId", produto.getCategoria().getId(), "marcaId", produto.getMarca().getId(), "imagemUrl", "javascript:alert(1)", "versao", 0)).statusCode()).isEqualTo(400);
        }
    }
    @Test
    void deveImpedirMudancaEExclusaoDeVarianteOfertada() throws Exception {
        ofertas.saveAndFlush(new Oferta(empresa, variante, new BigDecimal("100"), 2, true));
        try (var cliente = cliente(admin)) {
            assertThat(cliente.enviar("PUT", URL + "/variantes/" + variante.getId(), Map.of("produtoId", variante.getProduto().getId(), "armazenamentoGb", 256, "cor", "branco", "condicao", "NOVO", "atributos", Map.of(), "versao", 0)).statusCode()).isEqualTo(409);
            assertThat(cliente.enviar("DELETE", URL + "/variantes/" + variante.getId() + "?versao=0", null).statusCode()).isEqualTo(409);
            assertThat(variantes.findById(variante.getId()).orElseThrow().getCor()).isEqualTo("preto");
        }
    }
    @Test
    void deveAlterarAcessoInvalidarSessaoERecusarVersaoAntiga() throws Exception {
        try (var administrador = cliente(admin); var fornecedor = cliente(dono)) {
            assertThat(administrador.enviar("PUT", "/api/admin/usuarios/" + dono.getId() + "/acesso", Map.of("ativo", true, "perfil", "COMPRADOR", "versao", 0)).statusCode()).isEqualTo(200);
            assertThat(fornecedor.get("/api/auth/eu").statusCode()).isEqualTo(401);
            assertThat(administrador.enviar("PUT", "/api/admin/usuarios/" + dono.getId() + "/acesso", Map.of("ativo", true, "perfil", "FORNECEDOR", "versao", 0)).statusCode()).isEqualTo(409);
            assertThat(administrador.enviar("PUT", "/api/admin/usuarios/" + dono.getId() + "/acesso", Map.of("ativo", true, "perfil", "FORNECEDOR", "versao", 1)).statusCode()).isEqualTo(200);
        }
    }
    @Test
    void deveProtegerProprioAdministradorENaoCriarFornecedorSemEmpresa() throws Exception {
        var comprador = usuarios.saveAndFlush(new Usuario("Comprador", "comprador@teste.local", encoder.encode(SENHA), Perfil.COMPRADOR));
        try (var cliente = cliente(admin)) {
            assertThat(cliente.enviar("PUT", "/api/admin/usuarios/" + admin.getId() + "/acesso", Map.of("ativo", false, "perfil", "ADMIN", "versao", 0)).statusCode()).isEqualTo(409);
            assertThat(cliente.enviar("PUT", "/api/admin/usuarios/" + admin.getId() + "/acesso", Map.of("ativo", true, "perfil", "COMPRADOR", "versao", 0)).statusCode()).isEqualTo(409);
            assertThat(cliente.enviar("PUT", "/api/admin/usuarios/" + comprador.getId() + "/acesso", Map.of("ativo", true, "perfil", "FORNECEDOR", "versao", 0)).statusCode()).isEqualTo(409);
            assertThat(cliente.enviar("PUT", "/api/admin/usuarios/" + comprador.getId() + "/acesso", Map.of("ativo", true, "perfil", "ADMIN", "versao", 0)).statusCode()).isEqualTo(200);
        }
    }
    @Test
    void deveAtenderSolicitacaoUmaVezEVincularVarianteCompletaNoHistorico() throws Exception {
        try (var administrador = cliente(admin); var fornecedor = cliente(dono)) {
            var request = json(fornecedor, fornecedor.enviar("POST", "/api/fornecedor/catalogo/solicitacoes", Map.of("nomeProduto", "Produto pedido", "modelo", "Modelo teste", "marca", "Marca teste", "categoria", "Celulares", "caracteristicas", "256 GB preto novo")), 201);
            String path = "/api/admin/solicitacoes/" + request.get("id").asText() + "/resolucao";
            var resposta = Map.of("situacao", "ATENDIDA", "motivo", "Variante correspondente disponível no catálogo.", "varianteId", variante.getId(), "versao", 0);
            assertThat(administrador.enviar("PUT", path, resposta).statusCode()).isEqualTo(204);
            assertThat(administrador.enviar("PUT", path, resposta).statusCode()).isEqualTo(409);
            var historico = json(fornecedor, fornecedor.get("/api/fornecedor/catalogo/solicitacoes"), 200).get("itens").get(0);
            assertThat(historico.get("situacao").asText()).isEqualTo("ATENDIDA");
            assertThat(historico.get("varianteId").asText()).isEqualTo(variante.getId().toString());
            assertThat(historico.get("motivo").asText()).contains("disponível");
        }
    }
    @Test
    void deveValidarRecusaMotivoEVarianteIncompleta() throws Exception {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO compara_preco.solicitacao_catalogo(id, fornecedor_id, nome_produto, marca, modelo, categoria, caracteristicas) VALUES (?, ?, 'Pedido', 'Marca', 'Modelo', 'Categoria', 'Características detalhadas')", id, empresa.getId());
        var incompleta = variantes.saveAndFlush(new VarianteProduto(variante.getProduto(), null, "azul", CondicaoProduto.NOVO, Map.of()));
        try (var cliente = cliente(admin)) {
            String path = "/api/admin/solicitacoes/" + id + "/resolucao";
            assertThat(cliente.enviar("PUT", path, Map.of("situacao", "ATENDIDA", "motivo", "Atendido", "varianteId", incompleta.getId(), "versao", 0)).statusCode()).isEqualTo(409);
            assertThat(cliente.enviar("PUT", path, Map.of("situacao", "RECUSADA", "motivo", "x", "versao", 0)).statusCode()).isEqualTo(400);
            assertThat(cliente.enviar("PUT", path, Map.of("situacao", "RECUSADA", "motivo", "Dados insuficientes. Informe o armazenamento.", "versao", 0)).statusCode()).isEqualTo(204);
        }
    }
    @Test
    void deveContarElegibilidadeERelatorioSemDadosPessoais() throws Exception {
        var oferta = new Oferta(empresa, variante, new BigDecimal("100"), 2, true); oferta.moderar(ModeracaoOferta.APROVADA, null); ofertas.saveAndFlush(oferta);
        ofertas.saveAndFlush(new Oferta(empresa, outra, new BigDecimal("200"), 0, true));
        try (var cliente = cliente(admin)) {
            var dados = json(cliente, cliente.get("/api/admin/estatisticas"), 200);
            assertThat(dados.get("regioes").get(0).get("ofertasElegiveis").asInt()).isEqualTo(1);
            var csv = cliente.get("/api/admin/relatorios/regioes.csv");
            assertThat(csv.body()).contains("SP;1;1;1").doesNotContain(dono.getEmail(), empresa.getWhatsapp());
        }
    }
    @Test
    void deveVersionarConfiguracaoEAplicarAberturaNoContato() throws Exception {
        var oferta = new Oferta(empresa, variante, new BigDecimal("100"), 2, true); oferta.moderar(ModeracaoOferta.APROVADA, null); ofertas.saveAndFlush(oferta);
        try (var cliente = cliente(admin)) {
            var dados = json(cliente, cliente.enviar("PUT", "/api/admin/configuracoes", Map.of("aberturaContato", "Olá, tenho interesse neste produto:", "versao", 0)), 200);
            assertThat(dados.get("versao").asInt()).isEqualTo(1);
            assertThat(cliente.enviar("PUT", "/api/admin/configuracoes", Map.of("aberturaContato", "Mensagem antiga", "versao", 0)).statusCode()).isEqualTo(409);
            String contato = json(cliente, cliente.get("/api/catalogo/ofertas/" + oferta.getId() + "/contato"), 200).toString();
            assertThat(java.net.URLDecoder.decode(contato, StandardCharsets.UTF_8)).contains("Olá, tenho interesse neste produto:", "256 GB");
        }
    }
    @Test
    void deveManterUmAdministradorAtivoSobBloqueiosConcorrentes() throws Exception {
        var segundo = usuarios.saveAndFlush(new Usuario("Segundo admin", "segundo@teste.local", encoder.encode(SENHA), Perfil.ADMIN));
        try (var a = cliente(admin); var b = cliente(segundo); var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var inicio = new java.util.concurrent.CountDownLatch(1);
            var primeira = executor.submit(() -> { inicio.await(); return a.enviar("PUT", "/api/admin/usuarios/" + segundo.getId() + "/acesso", Map.of("ativo", false, "perfil", "ADMIN", "versao", 0)); });
            var segunda = executor.submit(() -> { inicio.await(); return b.enviar("PUT", "/api/admin/usuarios/" + admin.getId() + "/acesso", Map.of("ativo", false, "perfil", "ADMIN", "versao", 0)); });
            inicio.countDown();
            int s1 = primeira.get(20, java.util.concurrent.TimeUnit.SECONDS).statusCode();
            int s2 = segunda.get(20, java.util.concurrent.TimeUnit.SECONDS).statusCode();
            assertThat(java.util.List.of(s1,s2)).contains(200).allMatch(status -> java.util.Set.of(200,401,403).contains(status));
            assertThat(s1 == 200 && s2 == 200).isFalse();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM compara_preco.usuario WHERE perfil='ADMIN' AND ativo", Integer.class)).isEqualTo(1);
        }
    }
    @Test
    void deveRecusarImportacaoQuandoVarianteMudarAposPrevia() throws Exception {
        try (var cliente = cliente(dono)) {
            byte[] csv = ("varianteId;preco;estoque;disponivel\n" + variante.getId() + ";100;2;true\n").getBytes(StandardCharsets.UTF_8);
            var previa = json(cliente, cliente.arquivo("/api/fornecedor/importacoes", "ofertas.csv", csv, true), 200);
            variante.atualizar(variante.getProduto(), 256, "branco", CondicaoProduto.NOVO, Map.of()); variantes.saveAndFlush(variante);
            assertThat(cliente.enviar("POST", "/api/fornecedor/importacoes/" + previa.get("id").asText() + "/confirmacao", null).statusCode()).isEqualTo(409);
            assertThat(ofertas.count()).isZero();
        }
    }
    private ClienteApi cliente(Usuario usuario) throws Exception { var cliente = new ClienteApi(port, mapper); cliente.login(usuario.getEmail(), SENHA); return cliente; }
    private JsonNode json(ClienteApi cliente, java.net.http.HttpResponse<String> resposta, int status) {
        assertThat(resposta.statusCode()).withFailMessage(resposta.body()).isEqualTo(status); return cliente.json(resposta);
    }
}
