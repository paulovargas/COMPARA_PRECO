package br.com.comparapreco.catalogo.domain;

import java.util.Locale;

public final class TextoCatalogo {
    private TextoCatalogo() {}

    public static String obrigatorio(String valor, String campo, int tamanhoMaximo) {
        if (valor == null) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
        String texto = valor.replaceAll("(?U)\\s+", " ").strip();
        if (texto.isEmpty() || texto.length() > tamanhoMaximo
                || texto.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(campo + " deve conter entre 1 e " + tamanhoMaximo + " caracteres válidos.");
        }
        return texto;
    }

    public static String normalizado(String valor, String campo, int tamanhoMaximo) {
        return obrigatorio(valor, campo, tamanhoMaximo).toLowerCase(Locale.ROOT);
    }
}
