package br.com.comparapreco.conta.service;

import java.util.List;
import java.util.UUID;
import br.com.comparapreco.conta.dto.FornecedorSeguidoResponse;
import br.com.comparapreco.conta.dto.ProdutoFavoritoResponse;
import br.com.comparapreco.conta.repository.ColecaoRepository;
import br.com.comparapreco.conta.repository.ColecaoRepository.Tipo;
import br.com.comparapreco.shared.dto.PaginaResponse;
import br.com.comparapreco.shared.exception.ErroNegocioException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("isAuthenticated()")
@Transactional
public class ColecaoService {
    private final ColecaoRepository repository;
    public ColecaoService(ColecaoRepository repository) { this.repository = repository; }

    public boolean salvo(UUID usuario, UUID recurso, Tipo tipo) { return repository.salvo(usuario, recurso, tipo); }
    public void salvar(UUID usuario, UUID recurso, Tipo tipo) {
        if (repository.salvo(usuario, recurso, tipo)) { return; }
        if (repository.salvar(usuario, recurso, tipo) == 0 && !repository.salvo(usuario, recurso, tipo)) {
            throw new ErroNegocioException(404, "RECURSO_INDISPONIVEL", "Este item não está disponível para ser salvo.");
        }
    }
    public void remover(UUID usuario, UUID recurso, Tipo tipo) { repository.remover(usuario, recurso, tipo); }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PaginaResponse<ProdutoFavoritoResponse> produtos(UUID usuario, int pagina, int tamanho) {
        validarPagina(pagina, tamanho);
        return pagina(repository.produtos(usuario, pagina, tamanho), pagina, tamanho, repository.contar(usuario, Tipo.PRODUTO));
    }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PaginaResponse<FornecedorSeguidoResponse> fornecedores(UUID usuario, int pagina, int tamanho) {
        validarPagina(pagina, tamanho);
        return pagina(repository.fornecedores(usuario, pagina, tamanho), pagina, tamanho, repository.contar(usuario, Tipo.FORNECEDOR));
    }
    private void validarPagina(int pagina, int tamanho) {
        if (pagina < 0 || pagina > 1000000 || tamanho < 1 || tamanho > 50) {
            throw new ErroNegocioException(400, "PAGINACAO_INVALIDA", "Confira a página e o tamanho entre 1 e 50.");
        }
    }
    private <T> PaginaResponse<T> pagina(List<T> itens, int pagina, int tamanho, long total) {
        return new PaginaResponse<>(itens, pagina, tamanho, total, (int) ((total + tamanho - 1) / tamanho));
    }
}
