package br.com.comparapreco.shared.exception;

import java.time.Instant;
import java.util.Map;

public record ErroResponse(Instant instante, int status, String codigo, String mensagem, Map<String, String> campos) {
    public static ErroResponse de(int status, String codigo, String mensagem) {
        return new ErroResponse(Instant.now(), status, codigo, mensagem, Map.of());
    }
}
