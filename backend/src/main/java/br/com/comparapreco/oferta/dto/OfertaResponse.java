package br.com.comparapreco.oferta.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import br.com.comparapreco.catalogo.dto.VarianteResponse;
import br.com.comparapreco.oferta.entity.ModeracaoOferta;
import br.com.comparapreco.oferta.entity.Oferta;

public record OfertaResponse(UUID id, UUID fornecedorId, String fornecedor, VarianteResponse variante,
        BigDecimal preco, int estoque, boolean disponivel, ModeracaoOferta moderacao, String motivoModeracao,
        Instant criadoEm, Instant atualizadoEm, long versao) {
    public static OfertaResponse de(Oferta o) {
        return new OfertaResponse(o.getId(), o.getFornecedor().getId(), o.getFornecedor().getNomeFantasia(),
                VarianteResponse.de(o.getVariante()), o.getPreco(), o.getEstoque(), o.isDisponivel(),
                o.getModeracao(), o.getMotivoModeracao(), o.getCriadoEm(), o.getAtualizadoEm(), o.getVersao());
    }
}
