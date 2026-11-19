package br.com.comparapreco.importacao.dto;

import java.util.List;
import java.util.UUID;
import br.com.comparapreco.catalogo.dto.VarianteResponse;

public record LinhaImportacao(int numero, String varianteId, String preco, String estoque, String disponivel,
        VarianteResponse variante, String acao, UUID ofertaId, Long versao, List<String> erros) {}
