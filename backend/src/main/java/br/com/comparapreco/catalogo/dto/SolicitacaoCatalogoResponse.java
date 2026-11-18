package br.com.comparapreco.catalogo.dto;

import java.time.Instant;
import java.util.UUID;

public record SolicitacaoCatalogoResponse(UUID id, String nomeProduto, String marca, String modelo, String categoria,
        String caracteristicas, String situacao, String motivo, UUID varianteId, Instant criadoEm) {}
