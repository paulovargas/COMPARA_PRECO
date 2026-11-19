package br.com.comparapreco.support;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;

public class ClienteApi implements AutoCloseable {
    private final int port;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(Duration.ofSeconds(5)).build();
    private String token;
    private String cabecalho;
    public ClienteApi(int port, ObjectMapper mapper) throws Exception { this.port = port; this.mapper = mapper; renovarCsrf(); }
    public void renovarCsrf() throws Exception {
        var resposta = get("/api/auth/csrf");
        assertThat(resposta.statusCode()).isEqualTo(200);
        var csrf = json(resposta);
        token = csrf.get("token").asText(); cabecalho = csrf.get("cabecalho").asText();
    }
    public void login(String email, String senha) throws Exception {
        renovarCsrf();
        var response = enviar("POST", "/api/auth/login", Map.of("email", email, "senha", senha));
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
        renovarCsrf();
    }
    public HttpResponse<String> get(String path) throws Exception { return enviar("GET", path, null); }
    public HttpResponse<String> enviar(String method, String path, Object body) throws Exception {
        return enviar(method, path, body, true);
    }
    public HttpResponse<String> enviarSemCsrf(String method, String path, Object body) throws Exception {
        return enviar(method, path, body, false);
    }
    private HttpResponse<String> enviar(String method, String path, Object body, boolean csrf) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(15));
        if (csrf && !method.equals("GET")) { request.header(cabecalho, token); }
        if (body != null) { request.header("Content-Type", "application/json"); }
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    public HttpResponse<String> arquivo(String path, String nome, byte[] bytes, boolean csrf) throws Exception {
        String boundary = "ComparaPrecoTeste";
        var out = new java.io.ByteArrayOutputStream();
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"arquivo\"; filename=\"" + nome
                + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.write(bytes);
        out.write(("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (csrf) { request.header(cabecalho, token); }
        return http.send(request.POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
    }
    public JsonNode json(HttpResponse<String> response) { return mapper.readTree(response.body()); }
    @Override public void close() { http.close(); }
}
