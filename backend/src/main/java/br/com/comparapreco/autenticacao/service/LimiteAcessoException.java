package br.com.comparapreco.autenticacao.service;

public class LimiteAcessoException extends RuntimeException {
    private final int tentarNovamenteEm;
    public LimiteAcessoException(int tentarNovamenteEm) {
        super("Limite de tentativas atingido. Tente novamente mais tarde.");
        this.tentarNovamenteEm = tentarNovamenteEm;
    }

    public int getTentarNovamenteEm() {
        return tentarNovamenteEm;
    }
}
