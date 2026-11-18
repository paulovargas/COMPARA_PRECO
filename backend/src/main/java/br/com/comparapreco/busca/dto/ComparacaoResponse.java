package br.com.comparapreco.busca.dto;

import java.math.BigDecimal;
import br.com.comparapreco.catalogo.dto.VarianteResponse;
import br.com.comparapreco.shared.dto.PaginaResponse;

public record ComparacaoResponse(VarianteResponse variante, BigDecimal menorPreco, PaginaResponse<OfertaPublicaResponse> ofertas) {}
