package br.com.comparapreco.catalogo.service;

import java.util.UUID;
import br.com.comparapreco.catalogo.dto.SolicitacaoCatalogoRequest;
import br.com.comparapreco.catalogo.dto.SolicitacaoCatalogoResponse;
import br.com.comparapreco.catalogo.repository.SolicitacaoCatalogoRepository;
import br.com.comparapreco.fornecedor.service.FornecedorService;
import br.com.comparapreco.shared.dto.PaginaResponse;
import br.com.comparapreco.shared.exception.ErroNegocioException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('FORNECEDOR')")
public class SolicitacaoCatalogoService {
    private final SolicitacaoCatalogoRepository repository;
    private final FornecedorService fornecedores;
    public SolicitacaoCatalogoService(SolicitacaoCatalogoRepository repository, FornecedorService fornecedores) {
        this.repository = repository; this.fornecedores = fornecedores;
    }
    @Transactional
    public SolicitacaoCatalogoResponse criar(UUID usuarioId, SolicitacaoCatalogoRequest request) {
        // O bloqueio da empresa serializa limite e duplicidade, inclusive entre sessões diferentes.
        var empresa = fornecedores.buscarAprovadoParaOperacao(usuarioId);
        if (repository.pendenteIgual(empresa.getId(), request)) {
            throw new ErroNegocioException(409, "SOLICITACAO_DUPLICADA", "Você já possui uma solicitação pendente com estes dados.");
        }
        if (repository.contarPendentes(empresa.getId()) >= 10) {
            throw new ErroNegocioException(409, "LIMITE_SOLICITACOES", "Aguarde a análise das suas solicitações pendentes antes de enviar outras.");
        }
        return repository.criar(empresa.getId(), request);
    }
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public PaginaResponse<SolicitacaoCatalogoResponse> listar(UUID usuarioId, int pagina, int tamanho) {
        if (pagina < 0 || pagina > 1000000 || tamanho < 1 || tamanho > 50) {
            throw new ErroNegocioException(400, "PAGINACAO_INVALIDA", "Confira a página e o tamanho entre 1 e 50.");
        }
        var itens = repository.listar(usuarioId, pagina, tamanho);
        long total = repository.contarDoUsuario(usuarioId);
        return new PaginaResponse<>(itens, pagina, tamanho, total, (int) ((total + tamanho - 1) / tamanho));
    }
}
