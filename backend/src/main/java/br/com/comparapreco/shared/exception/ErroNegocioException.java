package br.com.comparapreco.shared.exception;

public class ErroNegocioException extends RuntimeException {
    private final int status;
    private final String codigo;

    public ErroNegocioException(int status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public int getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
