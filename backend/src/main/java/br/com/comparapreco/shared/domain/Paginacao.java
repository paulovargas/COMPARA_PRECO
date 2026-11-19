package br.com.comparapreco.shared.domain;

import br.com.comparapreco.shared.exception.ErroNegocioException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class Paginacao {
    private Paginacao() {}
    public static PageRequest criar(int pagina, int tamanho, String campo) {
        if (pagina < 0 || pagina > 1000000 || tamanho < 1 || tamanho > 50) {
            throw new ErroNegocioException(400, "PAGINACAO_INVALIDA", "Página inválida ou tamanho fora do intervalo de 1 a 50.");
        }
        return PageRequest.of(pagina, tamanho, Sort.by(Sort.Order.desc(campo), Sort.Order.asc("id")));
    }
}
