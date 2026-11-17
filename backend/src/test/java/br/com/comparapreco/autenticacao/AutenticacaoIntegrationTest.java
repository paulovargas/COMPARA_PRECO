package br.com.comparapreco.autenticacao;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import br.com.comparapreco.autenticacao.dto.LoginRequest;
import br.com.comparapreco.autenticacao.security.UsuarioSessao;
import br.com.comparapreco.autenticacao.service.LimiteAcessoException;
import br.com.comparapreco.autenticacao.service.LimiteAcessoService;
import br.com.comparapreco.support.PostgresIntegrationTest;
import br.com.comparapreco.usuario.dto.CadastroRequest;
import br.com.comparapreco.usuario.entity.Perfil;
import br.com.comparapreco.usuario.entity.Usuario;
import br.com.comparapreco.usuario.repository.UsuarioRepository;
import br.com.comparapreco.usuario.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.seguranca.limite-login-ip=100", "app.seguranca.limite-login-email=5", "app.seguranca.limite-cadastro-ip=3"})
class AutenticacaoIntegrationTest extends PostgresIntegrationTest {
    private static final String SENHA = "Senha-exclusiva-testes-123!";
    @Value("${local.server.port}") private int port;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private UsuarioService usuarioService;
    @Autowired private PasswordEncoder encoder;
    @Autowired private LimiteAcessoService limites;

    @BeforeEach
    void limparDadosDoBancoTemporario() {
        jdbc.update("DELETE FROM compara_preco.spring_session");
        usuarios.deleteAllInBatch();
        jdbc.update("DELETE FROM compara_preco.limite_acesso");
    }

    @Test
    void deveCadastrarCompradorNormalizarEmailEArmazenarSomenteHash() throws Exception {
        try (var cliente = new Cliente()) {
            var response = cliente.post("/api/auth/cadastro", Map.of("nome", "  Pessoa de Teste  ",
                    "email", "  PESSOA@TESTE.LOCAL ", "senha", SENHA, "perfil", "ADMIN"));
            assertThat(response.statusCode()).isEqualTo(201);
            var body = json(response);
            assertThat(body.get("perfil").asText()).isEqualTo("COMPRADOR");
            assertThat(body.get("email").asText()).isEqualTo("pessoa@teste.local");
            assertThat(body.get("nome").asText()).isEqualTo("Pessoa de Teste");
            assertThat(response.body()).doesNotContain("senha", "Hash", SENHA, "bcrypt");
            var salva = usuarios.findByEmail("pessoa@teste.local").orElseThrow();
            assertThat(salva.getSenhaHash()).startsWith("{bcrypt}").isNotEqualTo(SENHA);
            assertThat(encoder.matches(SENHA, salva.getSenhaHash())).isTrue();
            assertThat(mapper.writeValueAsString(salva)).doesNotContain("senhaHash", "bcrypt", SENHA);
            assertThat(cliente.get("/api/auth/eu").statusCode()).isEqualTo(401);
        }
    }

    @Test
    void deveRejeitarEmailDuplicadoIndependentementeDeMaiusculas() throws Exception {
        criarUsuario(Perfil.COMPRADOR, "pessoa@teste.local");
        try (var cliente = new Cliente()) {
            var response = cliente.post("/api/auth/cadastro", new CadastroRequest("Outra Pessoa", "PESSOA@TESTE.LOCAL", SENHA));
            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(json(response).get("codigo").asText()).isEqualTo("EMAIL_EM_USO");
            assertThat(usuarios.count()).isEqualTo(1);
        }
    }

    @Test
    void deveValidarCamposSemExporCredenciaisNosErros() throws Exception {
        try (var cliente = new Cliente()) {
            var response = cliente.post("/api/auth/cadastro", Map.of("nome", "", "email", "invalido", "senha", "curta"));
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(json(response).get("campos").has("senha")).isTrue();
            assertThat(response.body()).doesNotContain("curta");
            assertThat(usuarios.count()).isZero();
            assertThat(cliente.post("/api/auth/cadastro", new CadastroRequest("Pessoa", "pessoa@teste.local", "á".repeat(40)))
                    .statusCode()).isEqualTo(400);
        }
    }

    @Test
    void deveProtegerCadastroLoginELogoutComCsrfVinculadoASessao() throws Exception {
        try (var primeiro = new Cliente(); var segundo = new Cliente()) {
            assertThat(primeiro.enviar("POST", "/api/auth/cadastro", cadastro(), false).statusCode()).isEqualTo(403);
            assertThat(primeiro.enviar("POST", "/api/auth/login", new LoginRequest("pessoa@teste.local", SENHA), false)
                    .statusCode()).isEqualTo(403);
            assertThat(primeiro.enviar("POST", "/api/auth/logout", Map.of(), false).statusCode()).isEqualTo(403);
            segundo.token = primeiro.token;
            assertThat(segundo.post("/api/auth/cadastro", cadastro()).statusCode()).isEqualTo(403);
            assertThat(usuarios.count()).isZero();
        }
    }

    @Test
    void deveRetornarErroGenericoParaSenhaIncorretaContaInexistenteOuBloqueada() throws Exception {
        var usuario = criarUsuario(Perfil.COMPRADOR, "pessoa@teste.local");
        try (var cliente = new Cliente()) {
            var incorreta = cliente.post("/api/auth/login", new LoginRequest(usuario.getEmail(), "Senha-incorreta-123!"));
            var inexistente = cliente.post("/api/auth/login", new LoginRequest("ausente@teste.local", SENHA));
            jdbc.update("UPDATE compara_preco.usuario SET ativo = false WHERE id = ?", usuario.getId());
            var bloqueada = cliente.post("/api/auth/login", new LoginRequest(usuario.getEmail(), SENHA));
            for (var response : java.util.List.of(incorreta, inexistente, bloqueada)) {
                assertThat(response.statusCode()).isEqualTo(401);
                assertThat(json(response).get("codigo").asText()).isEqualTo("CREDENCIAIS_INVALIDAS");
                assertThat(json(response).get("mensagem").asText()).isEqualTo("Email ou senha inválidos.");
            }
        }
    }

    @Test
    void deveRotacionarSessaoECsrfEPersistirIdentidadeSemSenha() throws Exception {
        var usuario = criarUsuario(Perfil.COMPRADOR, "pessoa@teste.local");
        try (var cliente = new Cliente()) {
            String cookieAnterior = cliente.cookie();
            var login = cliente.post("/api/auth/login", new LoginRequest(usuario.getEmail(), SENHA));
            assertThat(login.statusCode()).isEqualTo(200);
            assertThat(cliente.cookie()).isNotEqualTo(cookieAnterior);
            assertThat(login.headers().allValues("Set-Cookie")).anyMatch(cookie -> cookie.contains("HttpOnly") && cookie.contains("SameSite=Lax"));
            assertThat(cliente.get("/api/auth/eu").statusCode()).isEqualTo(200);
            assertThat(cliente.post("/api/auth/logout", Map.of()).statusCode()).isEqualTo(403);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM compara_preco.spring_session WHERE principal_name = ?",
                    Integer.class, usuario.getEmail())).isEqualTo(1);
            byte[] bytes = jdbc.queryForObject("SELECT a.attribute_bytes FROM compara_preco.spring_session_attributes a "
                    + "JOIN compara_preco.spring_session s ON s.primary_id = a.session_primary_id "
                    + "WHERE s.principal_name = ? AND a.attribute_name = 'SPRING_SECURITY_CONTEXT'", byte[].class, usuario.getEmail());
            try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                var contexto = (SecurityContext) input.readObject();
                assertThat(contexto.getAuthentication().getCredentials()).isNull();
                assertThat(contexto.getAuthentication().getPrincipal()).isInstanceOf(UsuarioSessao.class);
                assertThat(contexto.getAuthentication().getName()).isEqualTo(usuario.getEmail());
            }
            cliente.renovarCsrf();
            assertThat(cliente.post("/api/auth/logout", Map.of()).statusCode()).isEqualTo(204);
        }
    }

    @Test
    void deveEncerrarSessaoERejeitarReutilizacaoDoCookie() throws Exception {
        var usuario = criarUsuario(Perfil.COMPRADOR, "pessoa@teste.local");
        try (var cliente = new Cliente()) {
            cliente.login(usuario);
            String cookie = cliente.cookie();
            assertThat(cliente.get("/api/auth/logout").statusCode()).isEqualTo(403);
            assertThat(cliente.get("/api/auth/eu").statusCode()).isEqualTo(200);
            assertThat(cliente.post("/api/auth/logout", Map.of()).statusCode()).isEqualTo(204);
            assertThat(cliente.get("/api/auth/eu").statusCode()).isEqualTo(401);
            cliente.reutilizarCookie(cookie);
            assertThat(cliente.get("/api/auth/eu").statusCode()).isEqualTo(401);
        }
    }

    @Test
    void deveRestringirAdministracaoPorPerfil() throws Exception {
        var comprador = criarUsuario(Perfil.COMPRADOR, "comprador@teste.local");
        var fornecedor = criarUsuario(Perfil.FORNECEDOR, "fornecedor@teste.local");
        var admin = criarUsuario(Perfil.ADMIN, "admin@teste.local");
        try (var cliente = new Cliente()) {
            assertThat(cliente.get("/api/admin/usuarios").statusCode()).isEqualTo(401);
            cliente.login(comprador);
            assertThat(cliente.get("/api/admin/usuarios").statusCode()).isEqualTo(403);
            assertThat(cliente.enviar("PATCH", "/api/admin/usuarios/" + admin.getId() + "/situacao", Map.of("ativo", false), true)
                    .statusCode()).isEqualTo(403);
            assertThat(cliente.get("/api/fornecedor/catalogo").statusCode()).isEqualTo(403);
            cliente.login(fornecedor);
            assertThat(cliente.get("/api/admin/usuarios").statusCode()).isEqualTo(403);
            cliente.login(admin);
            var lista = cliente.get("/api/admin/usuarios?tamanho=2");
            assertThat(lista.statusCode()).isEqualTo(200);
            assertThat(json(lista).get("itens").size()).isEqualTo(2);
            assertThat(json(lista).get("totalElementos").asInt()).isEqualTo(3);
            assertThat(lista.body()).doesNotContain("senhaHash", "bcrypt", SENHA);
        }
    }

    @Test
    void deveBloquearSessoesAbertasEExigirNovoLoginDepoisDeDesbloquear() throws Exception {
        var usuario = criarUsuario(Perfil.COMPRADOR, "pessoa@teste.local");
        var admin = criarUsuario(Perfil.ADMIN, "admin@teste.local");
        try (var comprador = new Cliente(); var administrador = new Cliente()) {
            comprador.login(usuario);
            administrador.login(admin);
            var bloqueio = administrador.enviar("PATCH", "/api/admin/usuarios/" + usuario.getId() + "/situacao", Map.of("ativo", false), true);
            assertThat(bloqueio.statusCode()).isEqualTo(200);
            assertThat(comprador.get("/api/auth/eu").statusCode()).isEqualTo(401);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM compara_preco.spring_session WHERE principal_name = ?",
                    Integer.class, usuario.getEmail())).isZero();
            comprador.renovarCsrf();
            assertThat(comprador.post("/api/auth/login", new LoginRequest(usuario.getEmail(), SENHA)).statusCode()).isEqualTo(401);
            assertThat(administrador.enviar("PATCH", "/api/admin/usuarios/" + usuario.getId() + "/situacao", Map.of("ativo", true), true)
                    .statusCode()).isEqualTo(200);
            assertThat(comprador.get("/api/auth/eu").statusCode()).isEqualTo(401);
            comprador.login(usuario);
            assertThat(comprador.get("/api/auth/eu").statusCode()).isEqualTo(200);
        }
    }

    @Test
    void deveRevalidarBloqueioNoBancoMesmoSemExclusaoPreviaDaSessao() throws Exception {
        var usuario = criarUsuario(Perfil.COMPRADOR, "pessoa@teste.local");
        try (var cliente = new Cliente()) {
            cliente.login(usuario);
            jdbc.update("UPDATE compara_preco.usuario SET ativo = false WHERE id = ?", usuario.getId());
            assertThat(cliente.get("/api/auth/eu").statusCode()).isEqualTo(401);
        }
    }

    @Test
    void deveRevogarPermissoesAntigasQuandoPerfilMudar() throws Exception {
        var admin = criarUsuario(Perfil.ADMIN, "admin@teste.local");
        try (var cliente = new Cliente()) {
            cliente.login(admin);
            jdbc.update("UPDATE compara_preco.usuario SET perfil = 'COMPRADOR' WHERE id = ?", admin.getId());
            assertThat(cliente.get("/api/admin/usuarios").statusCode()).isEqualTo(401);
            cliente.login(admin);
            assertThat(cliente.get("/api/admin/usuarios").statusCode()).isEqualTo(403);
        }
    }

    @Test
    void deveImpedirAutobloqueioEValidarAdministracao() throws Exception {
        var admin = criarUsuario(Perfil.ADMIN, "admin@teste.local");
        try (var cliente = new Cliente()) {
            cliente.login(admin);
            assertThat(cliente.enviar("PATCH", "/api/admin/usuarios/" + admin.getId() + "/situacao", Map.of("ativo", false), true)
                    .statusCode()).isEqualTo(409);
            assertThat(cliente.get("/api/auth/eu").statusCode()).isEqualTo(200);
            assertThat(cliente.get("/api/admin/usuarios?tamanho=51").statusCode()).isEqualTo(400);
            assertThat(cliente.enviar("PATCH", "/api/admin/usuarios/" + UUID.randomUUID() + "/situacao", Map.of("ativo", false), true)
                    .statusCode()).isEqualTo(404);
            assertThat(cliente.enviar("PATCH", "/api/admin/usuarios/" + admin.getId() + "/situacao", Map.of(), true)
                    .statusCode()).isEqualTo(400);
        }
    }

    @Test
    void deveRecusarSessaoExpiradaNoServidor() throws Exception {
        var usuario = criarUsuario(Perfil.COMPRADOR, "pessoa@teste.local");
        try (var cliente = new Cliente()) {
            cliente.login(usuario);
            jdbc.update("UPDATE compara_preco.spring_session SET expiry_time = 0, last_access_time = 0 WHERE principal_name = ?", usuario.getEmail());
            assertThat(cliente.get("/api/auth/eu").statusCode()).isEqualTo(401);
        }
    }

    @Test
    void deveLimitarLoginERenovarJanelaExpirada() throws Exception {
        try (var cliente = new Cliente()) {
            for (int tentativa = 0; tentativa < 5; tentativa++) {
                assertThat(cliente.post("/api/auth/login", new LoginRequest("ausente@teste.local", SENHA)).statusCode()).isEqualTo(401);
            }
            var limite = cliente.post("/api/auth/login", new LoginRequest("ausente@teste.local", SENHA));
            assertThat(limite.statusCode()).isEqualTo(429);
            assertThat(limite.headers().firstValue("Retry-After")).contains("600");
            jdbc.update("UPDATE compara_preco.limite_acesso SET janela_inicio = CURRENT_TIMESTAMP - INTERVAL '11 minutes'");
            assertThat(cliente.post("/api/auth/login", new LoginRequest("ausente@teste.local", SENHA)).statusCode()).isEqualTo(401);
        }
    }

    @Test
    void deveLimitarCadastroPorIp() throws Exception {
        try (var cliente = new Cliente()) {
            for (int tentativa = 0; tentativa < 3; tentativa++) {
                assertThat(cliente.post("/api/auth/cadastro", new CadastroRequest("Pessoa", "pessoa" + tentativa + "@teste.local", SENHA))
                        .statusCode()).isEqualTo(201);
            }
            assertThat(cliente.post("/api/auth/cadastro", cadastro()).statusCode()).isEqualTo(429);
        }
    }

    @Test
    void deveAplicarLimitePorEmailAtomicamenteEmChamadasConcorrentes() throws Exception {
        try (var executor = Executors.newFixedThreadPool(6)) {
            var tarefas = IntStream.range(0, 12).mapToObj(indice -> executor.submit(() -> {
                try { limites.verificarLogin("pessoa@teste.local", "ip-teste-" + indice); return true; }
                catch (LimiteAcessoException exception) { return false; }
            })).toList();
            int aceitas = 0;
            for (var tarefa : tarefas) { if (tarefa.get(10, TimeUnit.SECONDS)) { aceitas++; } }
            assertThat(aceitas).isEqualTo(5);
        }
    }

    @Test
    void deveAplicarLimiteDeLoginPorIpIndependentementeDoEmail() {
        for (int indice = 0; indice < 100; indice++) { limites.verificarLogin("pessoa" + indice + "@teste.local", "ip-teste"); }
        assertThatThrownBy(() -> limites.verificarLogin("outra@teste.local", "ip-teste")).isInstanceOf(LimiteAcessoException.class);
    }

    @Test
    void deveCriarAdministradorInicialUmaUnicaVezSemAlterarContaExistente() {
        usuarioService.criarAdministradorInicial(new CadastroRequest("Administrador", "ADMIN@TESTE.LOCAL", SENHA));
        usuarioService.criarAdministradorInicial(new CadastroRequest("Outro", "outro@teste.local", SENHA));
        assertThat(usuarios.count()).isEqualTo(1);
        var admin = usuarios.findByEmail("admin@teste.local").orElseThrow();
        assertThat(admin.getPerfil()).isEqualTo(Perfil.ADMIN);
        assertThat(encoder.matches(SENHA, admin.getSenhaHash())).isTrue();
    }

    @Test
    void deveImpedirPromocaoImplicitaDeCompradorPeloBootstrap() {
        criarUsuario(Perfil.COMPRADOR, "pessoa@teste.local");
        assertThatThrownBy(() -> usuarioService.criarAdministradorInicial(cadastro())).hasMessage("Já existe uma conta com este email.");
        assertThat(usuarios.findByEmail("pessoa@teste.local").orElseThrow().getPerfil()).isEqualTo(Perfil.COMPRADOR);
    }

    @Test
    void deveOmitirCredenciaisNaRepresentacaoDosRequests() {
        assertThat(cadastro().toString()).doesNotContain(SENHA, "pessoa@teste.local");
        assertThat(new LoginRequest("pessoa@teste.local", SENHA).toString()).doesNotContain(SENHA, "pessoa@teste.local");
    }

    private CadastroRequest cadastro() { return new CadastroRequest("Pessoa de Teste", "pessoa@teste.local", SENHA); }
    private Usuario criarUsuario(Perfil perfil, String email) {
        return usuarios.saveAndFlush(new Usuario("Pessoa de Teste", email, encoder.encode(SENHA), perfil));
    }
    private JsonNode json(HttpResponse<String> response) { return mapper.readTree(response.body()); }

    private class Cliente implements AutoCloseable {
        private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        private final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
        private String token;
        private String cabecalho;

        Cliente() throws Exception { renovarCsrf(); }
        void renovarCsrf() throws Exception {
            var response = get("/api/auth/csrf");
            assertThat(response.statusCode()).isEqualTo(200);
            var csrf = json(response);
            token = csrf.get("token").asText();
            cabecalho = csrf.get("cabecalho").asText();
        }
        void login(Usuario usuario) throws Exception {
            renovarCsrf();
            assertThat(post("/api/auth/login", new LoginRequest(usuario.getEmail(), SENHA)).statusCode()).isEqualTo(200);
            renovarCsrf();
        }
        String cookie() {
            return cookies.getCookieStore().getCookies().stream().filter(cookie -> cookie.getName().equals("SESSION") && !cookie.hasExpired())
                    .findFirst().orElseThrow().getValue();
        }
        void reutilizarCookie(String valor) {
            cookies.getCookieStore().removeAll();
            var cookie = new HttpCookie("SESSION", valor);
            cookie.setPath("/");
            cookie.setVersion(0);
            cookies.getCookieStore().add(URI.create("http://localhost:" + port), cookie);
        }
        HttpResponse<String> get(String path) throws Exception { return enviar("GET", path, null, false); }
        HttpResponse<String> post(String path, Object body) throws Exception { return enviar("POST", path, body, true); }
        HttpResponse<String> enviar(String method, String path, Object body, boolean csrf) throws Exception {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(15));
            if (csrf) { request.header(cabecalho, token); }
            if (body != null) { request.header("Content-Type", "application/json"); }
            request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
        @Override public void close() { http.close(); }
    }
}
