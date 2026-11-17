package br.com.comparapreco.catalogo.domain;

import java.util.ArrayList;
import java.util.List;
import br.com.comparapreco.catalogo.entity.VarianteProduto;
import br.com.comparapreco.shared.exception.ErroNegocioException;

public final class ValidacaoVarianteOferta {
    private ValidacaoVarianteOferta() {}
    public static List<String> pendencias(VarianteProduto variante) {
        var categoria = variante.getProduto().getCategoria();
        List<String> campos = new ArrayList<>();
        if (categoria.isExigeArmazenamento() && variante.getArmazenamentoGb() == null) { campos.add("armazenamentoGb"); }
        if (categoria.isExigeCor() && variante.getCor() == null) { campos.add("cor"); }
        categoria.getAtributosObrigatorios().stream().sorted()
                .filter(chave -> !variante.getAtributos().containsKey(chave)).forEach(chave -> campos.add("atributos." + chave));
        return List.copyOf(campos);
    }

    public static void exigirCompleta(VarianteProduto variante) {
        if (!pendencias(variante).isEmpty()) {
            throw new ErroNegocioException(409, "VARIANTE_INCOMPLETA", "A variante não atende às características obrigatórias da categoria.");
        }
    }
}
