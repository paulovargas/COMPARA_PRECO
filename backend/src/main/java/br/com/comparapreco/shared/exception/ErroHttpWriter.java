package br.com.comparapreco.shared.exception;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ErroHttpWriter {
    private final ObjectMapper mapper;
    public ErroHttpWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void escrever(HttpServletResponse response, int status, String codigo, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), ErroResponse.de(status, codigo, mensagem));
    }
}
