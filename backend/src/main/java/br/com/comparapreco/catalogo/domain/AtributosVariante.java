package br.com.comparapreco.catalogo.domain;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class AtributosVariante {
    private static final Set<String> CAMPOS_CENTRAIS = Set.of("cor", "condicao", "armazenamento_gb");

    private AtributosVariante() {}

    public static Map<String, String> normalizar(Map<String, String> atributos) {
        if (atributos == null || atributos.size() > 8) {
            throw new IllegalArgumentException("Informe um mapa com no máximo 8 atributos.");
        }
        Map<String, String> normalizados = new TreeMap<>();
        int tamanhoJson = 2;
        for (var atributo : atributos.entrySet()) {
            String chave = TextoCatalogo.normalizado(atributo.getKey(), "Chave do atributo", 32);
            String valor = TextoCatalogo.normalizado(atributo.getValue(), "Valor do atributo", 64);
            if (!chave.matches("[a-z][a-z0-9_]{0,31}") || CAMPOS_CENTRAIS.contains(chave)) {
                throw new IllegalArgumentException("Chave de atributo inválida ou reservada: " + chave);
            }
            if (normalizados.putIfAbsent(chave, valor) != null) {
                throw new IllegalArgumentException("Chaves de atributo equivalentes: " + chave);
            }
            // Reserva espaço para aspas, escapes e separadores na forma JSONB indexada.
            tamanhoJson += chave.length() + valor.getBytes(StandardCharsets.UTF_8).length + 8
                    + (int) valor.chars().filter(caractere -> caractere == 34 || caractere == 92).count();
        }
        if (tamanhoJson > 1400) {
            throw new IllegalArgumentException("Atributos excedem o tamanho permitido para comparação.");
        }
        return Map.copyOf(normalizados);
    }
}
