package br.com.comparapreco.shared.domain;

import br.com.comparapreco.shared.exception.ErroNegocioException;

public final class ControleVersao {
    private ControleVersao() {}
    public static void exigirAtual(long informada, long atual) {
        if (informada != atual) {
            throw new ErroNegocioException(409, "REGISTRO_DESATUALIZADO", "O registro foi alterado. Atualize os dados e tente novamente.");
        }
    }
}
