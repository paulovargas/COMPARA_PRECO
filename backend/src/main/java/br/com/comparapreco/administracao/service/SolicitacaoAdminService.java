package br.com.comparapreco.administracao.service;

import java.util.UUID;
import br.com.comparapreco.administracao.dto.SolicitacaoAdminDtos.*;
import br.com.comparapreco.administracao.repository.ControleAdministrativoRepository;
import br.com.comparapreco.catalogo.domain.ValidacaoVarianteOferta;
import br.com.comparapreco.catalogo.repository.SolicitacaoCatalogoRepository;
import br.com.comparapreco.catalogo.repository.VarianteProdutoRepository;
import br.com.comparapreco.shared.dto.PaginaResponse;
import br.com.comparapreco.shared.exception.ErroNegocioException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class SolicitacaoAdminService {
    private final SolicitacaoCatalogoRepository solicitacoes;
    private final VarianteProdutoRepository variantes;
    private final ControleAdministrativoRepository controle;
    public SolicitacaoAdminService(SolicitacaoCatalogoRepository solicitacoes, VarianteProdutoRepository variantes, ControleAdministrativoRepository controle) {
        this.solicitacoes = solicitacoes; this.variantes = variantes; this.controle = controle;
    }
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public PaginaResponse<SolicitacaoAdmin> listar(String situacao, int pagina, int tamanho) {
        if (pagina < 0 || pagina > 1000000 || tamanho < 1 || tamanho > 50 || situacao != null && !java.util.Set.of("PENDENTE", "ATENDIDA", "RECUSADA").contains(situacao)) {
            throw new ErroNegocioException(400, "DADOS_INVALIDOS", "Confira a situação e a paginação.");
        }
        return solicitacoes.listarAdministracao(situacao, pagina, tamanho);
    }
    @Transactional
    public void resolver(UUID id, Resolucao request) {
        controle.bloquearCatalogo();
        if (request.motivo().strip().length() < 3 || request.motivo().codePoints().anyMatch(Character::isISOControl)) {
            throw new ErroNegocioException(400, "MOTIVO_INVALIDO", "Informe uma resposta com 3 a 500 caracteres, sem controles.");
        }
        if (request.situacao() == Decisao.ATENDIDA) {
            if (request.varianteId() == null) { throw new ErroNegocioException(400, "VARIANTE_OBRIGATORIA", "Selecione a variante correspondente."); }
            var variante = variantes.findDetalhadaById(request.varianteId()).orElseThrow(() -> new ErroNegocioException(404, "VARIANTE_NAO_ENCONTRADA", "Variante não encontrada."));
            ValidacaoVarianteOferta.exigirCompleta(variante);
        } else if (request.varianteId() != null) { throw new ErroNegocioException(400, "DADOS_INVALIDOS", "Recusa não deve vincular variante."); }
        if (solicitacoes.resolver(id, request.situacao().name(), request.varianteId(), request.motivo().strip(), request.versao()) != 1) {
            throw new ErroNegocioException(409, "REGISTRO_DESATUALIZADO", "Solicitação já foi analisada ou está desatualizada. Recarregue a lista.");
        }
    }
}
