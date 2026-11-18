package br.com.comparapreco.busca.dto;

import java.math.BigDecimal;
import br.com.comparapreco.catalogo.dto.VarianteResponse;

public record VarianteBuscaResponse(VarianteResponse variante, String imagemUrl, BigDecimal menorPreco, long quantidadeOfertas) {}
