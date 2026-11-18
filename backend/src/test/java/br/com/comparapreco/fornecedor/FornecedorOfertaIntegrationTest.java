package br.com.comparapreco.fornecedor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import br.com.comparapreco.catalogo.entity.Categoria;
import br.com.comparapreco.catalogo.entity.CondicaoProduto;
import br.com.comparapreco.catalogo.entity.Marca;
import br.com.comparapreco.catalogo.entity.Produto;
import br.com.comparapreco.catalogo.entity.VarianteProduto;
import br.com.comparapreco.catalogo.repository.CategoriaRepository;
import br.com.comparapreco.catalogo.repository.MarcaRepository;
import br.com.comparapreco.catalogo.repository.ProdutoRepository;
import br.com.comparapreco.catalogo.repository.VarianteProdutoRepository;
import br.com.comparapreco.fornecedor.entity.Fornecedor;
import br.com.comparapreco.fornecedor.entity.SituacaoFornecedor;
import br.com.comparapreco.fornecedor.entity.UnidadeFederativa;
import br.com.comparapreco.fornecedor.repository.FornecedorRepository;
import br.com.comparapreco.oferta.entity.ModeracaoOferta;
import br.com.comparapreco.oferta.entity.Oferta;
import br.com.comparapreco.oferta.repository.OfertaRepository;
import br.com.comparapreco.support.ClienteApi;
import br.com.comparapreco.support.PostgresIntegrationTest;
import br.com.comparapreco.usuario.entity.Perfil;
import br.com.comparapreco.usuario.entity.Usuario;
import br.com.comparapreco.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FornecedorOfertaIntegrationTest extends PostgresIntegrationTest {
    private static final String SENHA = "Senha-exclusiva-testes-123!";
    @Value("${local.server.port}") private int port;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder encoder;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private FornecedorRepository fornecedores;
    @Autowired private OfertaRepository ofertas;
    @Autowired private CategoriaRepository categorias;
    @Autowired private MarcaRepository marcas;
    @Autowired private ProdutoRepository produtos;
    @Autowired private VarianteProdutoRepository variantes;
    private String hash;
    private Usuario admin;
    private Usuario donoA;
    private Usuario donoB;
    private Fornecedor empresaA;
    private Fornecedor empresaB;
    private Produto produto;
    private VarianteProduto variante;

    @BeforeEach
    void prepararBancoTemporario() {
        limpar();
        if (hash == null) { hash = encoder.encode(SENHA); }
        admin = usuario("admin@teste.local", Perfil.ADMIN);
        donoA = usuario("empresa-a@teste.local", Perfil.FORNECEDOR);
        donoB = usuario("empresa-b@teste.local", Perfil.FORNECEDOR);
        empresaA = empresa(donoA, "Empresa A"); empresaB = empresa(donoB, "Empresa B");
        var categoria = categorias.saveAndFlush(new Categoria("Smartphones teste", true, true, Set.of("ram_gb")));
        var marca = marcas.saveAndFlush(new Marca("Marca teste"));
        produto = produtos.saveAndFlush(new Produto(categoria, marca, "Produto teste", "Modelo teste"));
        variante = variantes.saveAndFlush(new VarianteProduto(produto, 256, "preto", CondicaoProduto.NOVO, Map.of("ram_gb", "8")));
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
    void deveCadastrarEmpresaDoUsuarioAutenticadoExigirNovoLoginEImpedirDuplicata() throws Exception {
        var comprador = usuario("comprador@teste.local", Perfil.COMPRADOR);
        try (var cliente = cliente(comprador)) {
            var dados = dadosEmpresa("Nova Empresa");
            dados.put("usuarioId", donoB.getId()); dados.put("situacao", "APROVADO");
            var response = cliente.enviar("POST", "/api/fornecedores", dados);
            var body = sucesso(cliente, response, 201);
            assertThat(body.get("usuarioId").asText()).isEqualTo(comprador.getId().toString());
            assertThat(body.get("situacao").asText()).isEqualTo("PENDENTE");
            assertThat(body.get("whatsapp").asText()).isEqualTo("5511999999999");
            assertThat(usuarios.findById(comprador.getId()).orElseThrow().getPerfil()).isEqualTo(Perfil.FORNECEDOR);
            assertThat(cliente.get("/api/auth/eu").statusCode()).isEqualTo(401);
            cliente.login(comprador.getEmail(), SENHA);
            assertThat(cliente.get("/api/fornecedor/perfil").statusCode()).isEqualTo(200);
            assertThat(cliente.enviar("POST", "/api/fornecedores", dados).statusCode()).isEqualTo(409);
            assertThat(cliente.enviar("POST", "/api/fornecedor/ofertas", dadosOferta()).statusCode()).isEqualTo(409);
        }
    }

    @Test
    void deveValidarWhatsAppUfECamposDaEmpresaSemPromoverConta() throws Exception {
        var comprador = usuario("comprador@teste.local", Perfil.COMPRADOR);
        try (var cliente = cliente(comprador)) {
            var dados = dadosEmpresa("Nova Empresa"); dados.put("whatsapp", "invalido");
            assertThat(cliente.enviar("POST", "/api/fornecedores", dados).statusCode()).isEqualTo(400);
            dados.put("whatsapp", "5511999999999"); dados.put("estado", "ZZ");
            assertThat(cliente.enviar("POST", "/api/fornecedores", dados).statusCode()).isEqualTo(400);
            assertThat(usuarios.findById(comprador.getId()).orElseThrow().getPerfil()).isEqualTo(Perfil.COMPRADOR);
        }
    }

    @Test
    void deveAprovarBloquearEExigirReanaliseComVersaoAtual() throws Exception {
        jdbc.update("UPDATE compara_preco.fornecedor SET situacao = 'PENDENTE' WHERE id = ?", empresaA.getId());
        try (var administrador = cliente(admin); var fornecedor = cliente(donoA)) {
            assertThat(fornecedor.get("/api/admin/fornecedores").statusCode()).isEqualTo(403);
            assertThat(administrador.get("/api/admin/fornecedores?situacao=PENDENTE").statusCode()).isEqualTo(200);
            var aprovada = moderarEmpresa(administrador, empresaA, "APROVADO", null, 0, 200);
            assertThat(aprovada.get("situacao").asText()).isEqualTo("APROVADO");
            moderarEmpresa(administrador, empresaA, "BLOQUEADO", "Revisão necessária", 0, 409);
            moderarEmpresa(administrador, empresaA, "BLOQUEADO", null, 1, 400);
            moderarEmpresa(administrador, empresaA, "BLOQUEADO", "Inválido" + (char) 0, 1, 400);
            moderarEmpresa(administrador, empresaA, "BLOQUEADO", "Revisão necessária", 1, 200);
            assertThat(fornecedor.enviar("POST", "/api/fornecedor/ofertas", dadosOferta()).statusCode()).isEqualTo(403);
            assertThat(fornecedor.get("/api/fornecedor/perfil").statusCode()).isEqualTo(200);
            moderarEmpresa(administrador, empresaA, "APROVADO", null, 2, 409);
            moderarEmpresa(administrador, empresaA, "PENDENTE", null, 2, 200);
            moderarEmpresa(administrador, empresaA, "APROVADO", null, 3, 200);
        }
    }

    @Test
    void deveExigirReaprovacaoApenasQuandoIdentidadeDaEmpresaMudar() throws Exception {
        ofertaAprovada(empresaA, "6500.00");
        try (var cliente = cliente(donoA)) {
            var dados = dadosEmpresa("Empresa A"); dados.put("cidade", "Campinas");
            var contato = sucesso(cliente, cliente.enviar("PUT", "/api/fornecedor/perfil", Map.of("empresa", dados, "versao", 0)), 200);
            assertThat(contato.get("situacao").asText()).isEqualTo("APROVADO");
            assertThat(cliente.enviar("PUT", "/api/fornecedor/perfil", Map.of("empresa", dados, "versao", 0)).statusCode()).isEqualTo(409);
            dados.put("nomeFantasia", "Nova Identidade");
            var identidade = sucesso(cliente, cliente.enviar("PUT", "/api/fornecedor/perfil", Map.of("empresa", dados, "versao", 1)), 200);
            assertThat(identidade.get("situacao").asText()).isEqualTo("PENDENTE");
            assertThat(ofertas.buscarElegiveis(variante.getId())).isEmpty();
        }
    }

    @Test
    void deveIsolarCrudPorProprietarioERejeitarVersoesDesatualizadas() throws Exception {
        try (var a = cliente(donoA); var b = cliente(donoB)) {
            var payload = dadosOferta(); payload.put("fornecedorId", empresaB.getId()); payload.put("moderacao", "APROVADA");
            var criada = sucesso(a, a.enviar("POST", "/api/fornecedor/ofertas", payload), 201);
            String path = "/api/fornecedor/ofertas/" + criada.get("id").asText();
            assertThat(criada.get("fornecedorId").asText()).isEqualTo(empresaA.getId().toString());
            assertThat(criada.get("moderacao").asText()).isEqualTo("PENDENTE");
            assertThat(b.get(path).statusCode()).isEqualTo(404);
            assertThat(b.enviar("PUT", path, atualizarOferta("1.00", 0)).statusCode()).isEqualTo(404);
            assertThat(b.enviar("DELETE", path + "?versao=0", null).statusCode()).isEqualTo(404);
            assertThat(b.json(b.get("/api/fornecedor/ofertas")).get("totalElementos").asInt()).isZero();
            var mudanca = atualizarOferta("6400.00", 0); mudanca.put("varianteId", UUID.randomUUID()); mudanca.put("moderacao", "APROVADA");
            var atualizada = sucesso(a, a.enviar("PUT", path, mudanca), 200);
            assertThat(atualizada.get("variante").get("id").asText()).isEqualTo(variante.getId().toString());
            assertThat(atualizada.get("moderacao").asText()).isEqualTo("PENDENTE");
            var versaoFracionaria = atualizarOferta("6300.00", 1); versaoFracionaria.put("versao", new BigDecimal("1.5"));
            assertThat(a.enviar("PUT", path, versaoFracionaria).statusCode()).isEqualTo(400);
            assertThat(a.enviar("PUT", path, atualizarOferta("6300.00", 0)).statusCode()).isEqualTo(409);
            assertThat(a.enviar("DELETE", path + "?versao=0", null).statusCode()).isEqualTo(409);
            assertThat(a.enviar("DELETE", path + "?versao=1", null).statusCode()).isEqualTo(204);
            assertThat(a.get(path).statusCode()).isEqualTo(404);
        }
    }

    @Test
    void deveImpedirOfertaDuplicadaDaEmpresaMasPermitirOutroFornecedorNaMesmaVariante() throws Exception {
        try (var a = cliente(donoA); var b = cliente(donoB)) {
            assertThat(a.enviar("POST", "/api/fornecedor/ofertas", dadosOferta()).statusCode()).isEqualTo(201);
            assertThat(a.enviar("POST", "/api/fornecedor/ofertas", dadosOferta()).statusCode()).isEqualTo(409);
            assertThat(b.enviar("POST", "/api/fornecedor/ofertas", dadosOferta()).statusCode()).isEqualTo(201);
            assertThat(ofertas.count()).isEqualTo(2);
            assertThat(ofertas.buscarElegiveis(variante.getId())).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.001", "10000000000"})
    void deveRejeitarPrecoInvalidoSemArredondar(String preco) throws Exception {
        try (var a = cliente(donoA)) {
            var dados = dadosOferta(); dados.put("preco", new BigDecimal(preco));
            assertThat(a.enviar("POST", "/api/fornecedor/ofertas", dados).statusCode()).isEqualTo(400);
            assertThat(ofertas.count()).isZero();
        }
    }

    @Test
    void deveValidarEstoqueDisponibilidadeEVarianteExistente() throws Exception {
        try (var a = cliente(donoA)) {
            var dados = dadosOferta(); dados.put("estoque", -1);
            assertThat(a.enviar("POST", "/api/fornecedor/ofertas", dados).statusCode()).isEqualTo(400);
            dados.put("estoque", new BigDecimal("1.5"));
            assertThat(a.enviar("POST", "/api/fornecedor/ofertas", dados).statusCode()).isEqualTo(400);
            dados.put("estoque", 3); dados.put("disponivel", null);
            assertThat(a.enviar("POST", "/api/fornecedor/ofertas", dados).statusCode()).isEqualTo(400);
            dados.put("disponivel", true); dados.put("varianteId", UUID.randomUUID());
            assertThat(a.enviar("POST", "/api/fornecedor/ofertas", dados).statusCode()).isEqualTo(404);
        }
    }

    @ParameterizedTest
    @MethodSource("caracteristicasIncompletas")
    void deveRejeitarVariantesIncompletasEInformarPendenciasNoCatalogo(Integer armazenamento, String cor, Map<String, String> atributos) throws Exception {
        var incompleta = variantes.saveAndFlush(new VarianteProduto(produto, armazenamento, cor, CondicaoProduto.NOVO, atributos));
        try (var a = cliente(donoA)) {
            var dados = dadosOferta(); dados.put("varianteId", incompleta.getId());
            var response = a.enviar("POST", "/api/fornecedor/ofertas", dados);
            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(a.json(response).get("codigo").asText()).isEqualTo("VARIANTE_INCOMPLETA");
            var catalogo = sucesso(a, a.get("/api/fornecedor/catalogo/variantes?texto=Modelo"), 200);
            assertThat(catalogo.get("totalElementos").asInt()).isEqualTo(2);
            assertThat(catalogo.get("itens").toString()).contains("pendencias", "false");
        }
    }
    static Stream<Arguments> caracteristicasIncompletas() {
        return Stream.of(Arguments.of(null, "preto", Map.of("ram_gb", "8")),
                Arguments.of(256, null, Map.of("ram_gb", "8")), Arguments.of(256, "preto", Map.of()));
    }

    @Test
    void devePreservarModeracaoDuranteEdicaoEExigirReanaliseDeOfertaBloqueada() throws Exception {
        try (var a = cliente(donoA); var administrador = cliente(admin)) {
            var criada = sucesso(a, a.enviar("POST", "/api/fornecedor/ofertas", dadosOferta()), 201);
            UUID id = UUID.fromString(criada.get("id").asText());
            String path = "/api/fornecedor/ofertas/" + id;
            assertThat(a.enviar("PATCH", "/api/admin/ofertas/" + id + "/moderacao", Map.of("moderacao", "APROVADA", "versao", 0)).statusCode()).isEqualTo(403);
            moderarOferta(administrador, id, "APROVADA", null, 0, 200);
            var editada = sucesso(a, a.enviar("PUT", path, atualizarOferta("6400.00", 1)), 200);
            assertThat(editada.get("moderacao").asText()).isEqualTo("APROVADA");
            moderarOferta(administrador, id, "BLOQUEADA", null, 2, 400);
            moderarOferta(administrador, id, "BLOQUEADA", "Revisar anúncio", 2, 200);
            var bloqueada = sucesso(a, a.enviar("PUT", path, atualizarOferta("6300.00", 3)), 200);
            assertThat(bloqueada.get("moderacao").asText()).isEqualTo("BLOQUEADA");
            assertThat(ofertas.buscarElegiveis(variante.getId())).isEmpty();
            moderarOferta(administrador, id, "APROVADA", null, 4, 409);
            moderarOferta(administrador, id, "PENDENTE", null, 4, 200);
            moderarOferta(administrador, id, "APROVADA", null, 5, 200);
            assertThat(ofertas.buscarElegiveis(variante.getId())).hasSize(1);
            assertThat(administrador.get("/api/admin/ofertas").statusCode()).isEqualTo(200);
            assertThat(administrador.get("/api/admin/ofertas?moderacao=APROVADA&fornecedorId=" + empresaA.getId()).statusCode()).isEqualTo(200);
        }
    }

    @Test
    void deveExcluirDaElegibilidadeOfertasIndisponiveisBloqueiosEVariantesIncompletas() {
        var oferta = ofertaAprovada(empresaA, "6500.00");
        assertThat(ofertas.buscarElegiveis(variante.getId())).hasSize(1);
        jdbc.update("UPDATE compara_preco.fornecedor SET situacao='BLOQUEADO', motivo_moderacao='Revisão' WHERE id=?", empresaA.getId());
        assertThat(ofertas.buscarElegiveis(variante.getId())).isEmpty();
        jdbc.update("UPDATE compara_preco.fornecedor SET situacao='APROVADO', motivo_moderacao=NULL WHERE id=?", empresaA.getId());
        jdbc.update("UPDATE compara_preco.usuario SET ativo=false WHERE id=?", donoA.getId());
        assertThat(ofertas.buscarElegiveis(variante.getId())).isEmpty();
        jdbc.update("UPDATE compara_preco.usuario SET ativo=true, perfil='COMPRADOR' WHERE id=?", donoA.getId());
        assertThat(ofertas.buscarElegiveis(variante.getId())).isEmpty();
        jdbc.update("UPDATE compara_preco.usuario SET perfil='FORNECEDOR' WHERE id=?", donoA.getId());
        jdbc.update("UPDATE compara_preco.oferta SET estoque=0 WHERE id=?", oferta.getId());
        assertThat(ofertas.buscarElegiveis(variante.getId())).isEmpty();
        jdbc.update("UPDATE compara_preco.oferta SET estoque=3, disponivel=false WHERE id=?", oferta.getId());
        assertThat(ofertas.buscarElegiveis(variante.getId())).isEmpty();
        jdbc.update("UPDATE compara_preco.oferta SET disponivel=true, moderacao='PENDENTE' WHERE id=?", oferta.getId());
        assertThat(ofertas.buscarElegiveis(variante.getId())).isEmpty();
        jdbc.update("UPDATE compara_preco.oferta SET moderacao='APROVADA' WHERE id=?", oferta.getId());
        jdbc.update("UPDATE compara_preco.categoria SET atributos_obrigatorios='[\"ram_gb\",\"nfc\"]'::jsonb WHERE id=?", produto.getCategoria().getId());
        assertThat(ofertas.buscarElegiveis(variante.getId())).isEmpty();
        jdbc.update("UPDATE compara_preco.categoria SET atributos_obrigatorios='[\"ram_gb\"]'::jsonb WHERE id=?", produto.getCategoria().getId());
        assertThat(ofertas.buscarElegiveis(variante.getId())).hasSize(1);
    }

    @Test
    void deveOrdenarElegiveisPorPrecoEntreFornecedoresDaMesmaVariante() {
        ofertaAprovada(empresaA, "6500.00"); ofertaAprovada(empresaB, "6200.00");
        assertThat(ofertas.buscarElegiveis(variante.getId())).extracting(Oferta::getPreco)
                .containsExactly(new BigDecimal("6200.00"), new BigDecimal("6500.00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.001", "10000000000"})
    void deveRejeitarPrecosInvalidosTambemPorSqlDireto(String preco) {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO compara_preco.oferta (id, fornecedor_id, variante_id, preco, estoque, disponivel) VALUES (?, ?, ?, ?, 1, true)",
                UUID.randomUUID(), empresaA.getId(), variante.getId(), new BigDecimal(preco))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveImpedirDuplicatasDeOfertaPorRestricaoDoBanco() {
        ofertaAprovada(empresaA, "6500.00");
        assertThatThrownBy(() -> ofertas.saveAndFlush(new Oferta(empresaA, variante, new BigDecimal("6200.00"), 1, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveBuscarPublicamenteECompararSomenteMesmaVarianteSemExporDadosPrivados() throws Exception {
        ofertaAprovada(empresaA, "6500.00"); ofertaAprovada(empresaB, "6200.00");
        var outra = variantes.saveAndFlush(new VarianteProduto(produto, 512, "preto", CondicaoProduto.NOVO, Map.of("ram_gb", "8")));
        var oferta = new Oferta(empresaA, outra, new BigDecimal("100.00"), 1, true);
        oferta.moderar(ModeracaoOferta.APROVADA, null); ofertas.saveAndFlush(oferta);
        try (var publico = new ClienteApi(port, mapper)) {
            var busca = sucesso(publico, publico.get("/api/catalogo/variantes?texto=MODELO"), 200);
            assertThat(busca.get("totalElementos").asInt()).isEqualTo(2);
            assertThat(busca.get("itens").get(0).get("menorPreco").decimalValue()).isEqualByComparingTo("100");
            var comparacao = sucesso(publico, publico.get("/api/catalogo/variantes/" + variante.getId() + "/ofertas"), 200);
            assertThat(comparacao.get("menorPreco").decimalValue()).isEqualByComparingTo("6200");
            assertThat(comparacao.get("ofertas").get("totalElementos").asInt()).isEqualTo(2);
            var itens = comparacao.get("ofertas").get("itens");
            assertThat(itens.get(0).get("menorPreco").asBoolean()).isTrue();
            assertThat(itens.get(1).get("menorPreco").asBoolean()).isFalse();
            assertThat(itens.toString()).doesNotContain("email", "usuarioId", "motivoModeracao", "razaoSocial", "whatsapp", "versao");
        }
    }

    @Test
    void deveDestacarEmpatesEntrePaginasEManterOrdemEstavel() throws Exception {
        ofertaAprovada(empresaA, "6200.00"); ofertaAprovada(empresaB, "6200.00");
        try (var publico = new ClienteApi(port, mapper)) {
            String path = "/api/catalogo/variantes/" + variante.getId() + "/ofertas?tamanho=1&pagina=";
            var primeira = sucesso(publico, publico.get(path + "0"), 200).get("ofertas");
            var segunda = sucesso(publico, publico.get(path + "1"), 200).get("ofertas");
            assertThat(primeira.get("totalPaginas").asInt()).isEqualTo(2);
            assertThat(primeira.get("itens").get(0).get("menorPreco").asBoolean()).isTrue();
            assertThat(segunda.get("itens").get(0).get("menorPreco").asBoolean()).isTrue();
            assertThat(primeira.get("itens").get(0).get("id").asText()).isNotEqualTo(segunda.get("itens").get(0).get("id").asText());
            assertThat(sucesso(publico, publico.get(path + "1"), 200).get("ofertas")).isEqualTo(segunda);
            assertThat(sucesso(publico, publico.get(path + "2"), 200).get("ofertas").get("itens").size()).isZero();
        }
    }

    @Test
    void deveAplicarTodosFiltrosECalcularPrecoEContagemNoMesmoConjunto() throws Exception {
        ofertaAprovada(empresaA, "6500.00"); ofertaAprovada(empresaB, "6200.00");
        jdbc.update("UPDATE compara_preco.fornecedor SET estado='RJ', cidade='Niterói' WHERE id=?", empresaB.getId());
        String filtros = "?texto=MARCA&categoriaId=" + produto.getCategoria().getId() + "&marcaId=" + produto.getMarca().getId()
                + "&modelo=MODELO%20teste&armazenamentoGb=256&cor=PRETO&condicao=NOVO&estado=SP&cidade=S%C3%A3o%20Paulo&fornecedorId=" + empresaA.getId();
        try (var publico = new ClienteApi(port, mapper)) {
            var busca = sucesso(publico, publico.get("/api/catalogo/variantes" + filtros), 200);
            assertThat(busca.get("totalElementos").asInt()).isEqualTo(1);
            assertThat(busca.get("itens").get(0).get("quantidadeOfertas").asInt()).isEqualTo(1);
            assertThat(busca.get("itens").get(0).get("menorPreco").decimalValue()).isEqualByComparingTo("6500");
            var comparacao = sucesso(publico, publico.get("/api/catalogo/variantes/" + variante.getId() + "/ofertas" + filtros), 200);
            assertThat(comparacao.get("menorPreco").decimalValue()).isEqualByComparingTo("6500");
            assertThat(comparacao.get("ofertas").get("totalElementos").asInt()).isEqualTo(1);
            for (String filtro : new String[]{"armazenamentoGb=512", "cor=azul", "condicao=USADO", "modelo=outro", "estado=AC", "cidade=Outra", "marcaId=" + UUID.randomUUID(), "categoriaId=" + UUID.randomUUID(), "fornecedorId=" + UUID.randomUUID(), "texto=%25", "texto=%27%20OR%201%3D1--"}) {
                assertThat(sucesso(publico, publico.get("/api/catalogo/variantes?" + filtro), 200).get("totalElementos").asInt()).isZero();
            }
        }
    }

    @Test
    void deveRetirarOfertasDaBuscaComparacaoEOpcoesAposPerdaDeElegibilidade() throws Exception {
        var oferta = ofertaAprovada(empresaA, "6500.00");
        try (var publico = new ClienteApi(port, mapper)) {
            assertThat(sucesso(publico, publico.get("/api/catalogo/categorias"), 200).size()).isEqualTo(1);
            assertThat(sucesso(publico, publico.get("/api/catalogo/marcas"), 200).size()).isEqualTo(1);
            for (String alteracao : new String[]{
                    "UPDATE compara_preco.oferta SET estoque=0",
                    "UPDATE compara_preco.oferta SET estoque=3, disponivel=false",
                    "UPDATE compara_preco.oferta SET disponivel=true, moderacao='PENDENTE'",
                    "UPDATE compara_preco.oferta SET moderacao='BLOQUEADA', motivo_moderacao='Revisão'",
                    "UPDATE compara_preco.fornecedor SET situacao='BLOQUEADO', motivo_moderacao='Revisão'",
                    "UPDATE compara_preco.usuario SET ativo=false WHERE perfil='FORNECEDOR'",
                    "UPDATE compara_preco.usuario SET perfil='COMPRADOR' WHERE perfil='FORNECEDOR'",
                    "UPDATE compara_preco.categoria SET atributos_obrigatorios='[\"nfc\"]'::jsonb"}) {
                jdbc.update(alteracao);
                assertThat(sucesso(publico, publico.get("/api/catalogo/variantes"), 200).get("totalElementos").asInt()).isZero();
                assertThat(sucesso(publico, publico.get("/api/catalogo/categorias"), 200).size()).isZero();
                assertThat(sucesso(publico, publico.get("/api/catalogo/marcas"), 200).size()).isZero();
                // A categoria incompleta torna o recurso inteiro indisponível.
                var resposta = publico.get("/api/catalogo/variantes/" + variante.getId() + "/ofertas");
                if (alteracao.contains("atributos_obrigatorios")) { assertThat(resposta.statusCode()).isEqualTo(404); }
                else {
                    var comparacao = sucesso(publico, resposta, 200);
                    assertThat(comparacao.get("menorPreco").isNull()).isTrue();
                    assertThat(comparacao.get("ofertas").get("totalElementos").asInt()).isZero();
                }
                jdbc.update("UPDATE compara_preco.oferta SET estoque=3, disponivel=true, moderacao='APROVADA', motivo_moderacao=NULL WHERE id=?", oferta.getId());
                jdbc.update("UPDATE compara_preco.fornecedor SET situacao='APROVADO', motivo_moderacao=NULL");
                jdbc.update("UPDATE compara_preco.usuario SET ativo=true, perfil='FORNECEDOR' WHERE id IN (?, ?)", donoA.getId(), donoB.getId());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"pagina=-1", "pagina=1000001", "tamanho=0", "tamanho=51", "pagina=1.5", "armazenamentoGb=0", "estado=XX", "condicao=INVALIDA", "marcaId=invalido", "texto=%00"})
    void deveValidarFiltrosPublicos(String filtro) throws Exception {
        try (var publico = new ClienteApi(port, mapper)) {
            assertThat(publico.get("/api/catalogo/variantes?" + filtro).statusCode()).isEqualTo(400);
        }
    }

    @Test
    void deveResponderComparacaoVaziaEVarianteInexistenteSemExigirLogin() throws Exception {
        try (var publico = new ClienteApi(port, mapper)) {
            var vazio = sucesso(publico, publico.get("/api/catalogo/variantes/" + variante.getId() + "/ofertas"), 200);
            assertThat(vazio.get("menorPreco").isNull()).isTrue();
            assertThat(vazio.get("ofertas").get("totalPaginas").asInt()).isZero();
            assertThat(publico.get("/api/catalogo/variantes/" + UUID.randomUUID() + "/ofertas").statusCode()).isEqualTo(404);
            assertThat(publico.enviar("POST", "/api/catalogo/variantes", Map.of()).statusCode()).isEqualTo(401);
        }
    }

    private Usuario usuario(String email, Perfil perfil) { return usuarios.saveAndFlush(new Usuario("Pessoa de teste", email, hash, perfil)); }
    private Fornecedor empresa(Usuario dono, String nome) {
        var empresa = new Fornecedor(dono, nome + " Ltda", nome, "Descrição", "5511999999999", "São Paulo", UnidadeFederativa.SP);
        empresa.moderar(SituacaoFornecedor.APROVADO, null);
        return fornecedores.saveAndFlush(empresa);
    }
    private Oferta ofertaAprovada(Fornecedor empresa, String preco) {
        var oferta = new Oferta(empresa, variante, new BigDecimal(preco), 3, true);
        oferta.moderar(ModeracaoOferta.APROVADA, null);
        return ofertas.saveAndFlush(oferta);
    }
    private ClienteApi cliente(Usuario usuario) throws Exception {
        var cliente = new ClienteApi(port, mapper); cliente.login(usuario.getEmail(), SENHA); return cliente;
    }
    private Map<String, Object> dadosEmpresa(String nome) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("razaoSocial", nome + " Ltda"); dados.put("nomeFantasia", nome); dados.put("descricao", "Descrição");
        dados.put("whatsapp", "+55 (11) 99999-9999"); dados.put("cidade", "São Paulo"); dados.put("estado", "SP"); return dados;
    }
    private Map<String, Object> dadosOferta() {
        return new HashMap<>(Map.of("varianteId", variante.getId(), "preco", new BigDecimal("6500.00"), "estoque", 3, "disponivel", true));
    }
    private Map<String, Object> atualizarOferta(String preco, long versao) {
        return new HashMap<>(Map.of("preco", new BigDecimal(preco), "estoque", 3, "disponivel", true, "versao", versao));
    }
    private JsonNode moderarEmpresa(ClienteApi cliente, Fornecedor empresa, String situacao, String motivo, long versao, int status) throws Exception {
        var dados = new HashMap<String, Object>(); dados.put("situacao", situacao); dados.put("motivo", motivo); dados.put("versao", versao);
        return sucesso(cliente, cliente.enviar("PATCH", "/api/admin/fornecedores/" + empresa.getId() + "/situacao", dados), status);
    }
    private JsonNode moderarOferta(ClienteApi cliente, UUID id, String moderacao, String motivo, long versao, int status) throws Exception {
        var dados = new HashMap<String, Object>(); dados.put("moderacao", moderacao); dados.put("motivo", motivo); dados.put("versao", versao);
        return sucesso(cliente, cliente.enviar("PATCH", "/api/admin/ofertas/" + id + "/moderacao", dados), status);
    }
    private JsonNode sucesso(ClienteApi cliente, java.net.http.HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(status); return cliente.json(response);
    }
}
