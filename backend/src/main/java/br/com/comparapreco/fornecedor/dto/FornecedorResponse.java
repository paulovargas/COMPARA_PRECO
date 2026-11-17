package br.com.comparapreco.fornecedor.dto;

import java.time.Instant;
import java.util.UUID;
import br.com.comparapreco.fornecedor.entity.Fornecedor;
import br.com.comparapreco.fornecedor.entity.SituacaoFornecedor;
import br.com.comparapreco.fornecedor.entity.UnidadeFederativa;

public record FornecedorResponse(UUID id, UUID usuarioId, String razaoSocial, String nomeFantasia, String descricao,
        String whatsapp, String cidade, UnidadeFederativa estado, SituacaoFornecedor situacao, String motivoModeracao,
        Instant criadoEm, Instant atualizadoEm, long versao) {
    public static FornecedorResponse de(Fornecedor f) {
        return new FornecedorResponse(f.getId(), f.getUsuario().getId(), f.getRazaoSocial(), f.getNomeFantasia(),
                f.getDescricao(), f.getWhatsapp(), f.getCidade(), f.getEstado(), f.getSituacao(), f.getMotivoModeracao(),
                f.getCriadoEm(), f.getAtualizadoEm(), f.getVersao());
    }
}
