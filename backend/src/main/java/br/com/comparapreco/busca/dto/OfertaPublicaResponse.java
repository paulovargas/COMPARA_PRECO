package br.com.comparapreco.busca.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OfertaPublicaResponse(UUID id, UUID fornecedorId, String fornecedor, String cidade, String estado,
        BigDecimal preco, int estoque, Instant atualizadoEm, boolean menorPreco) {}
