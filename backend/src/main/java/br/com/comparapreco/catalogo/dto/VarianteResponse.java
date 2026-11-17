package br.com.comparapreco.catalogo.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import br.com.comparapreco.catalogo.domain.ValidacaoVarianteOferta;
import br.com.comparapreco.catalogo.entity.CondicaoProduto;
import br.com.comparapreco.catalogo.entity.VarianteProduto;

public record VarianteResponse(UUID id, UUID produtoId, String nomeProduto, String modelo, String marca, String categoria,
        Integer armazenamentoGb, String cor, CondicaoProduto condicao, Map<String, String> atributos, boolean completa, List<String> pendencias) {
    public static VarianteResponse de(VarianteProduto v) {
        var p = v.getProduto();
        return new VarianteResponse(v.getId(), p.getId(), p.getNome(), p.getModelo(), p.getMarca().getNome(),
                p.getCategoria().getNome(), v.getArmazenamentoGb(), v.getCor(), v.getCondicao(), v.getAtributos(), ValidacaoVarianteOferta.pendencias(v).isEmpty(), ValidacaoVarianteOferta.pendencias(v));
    }
}
