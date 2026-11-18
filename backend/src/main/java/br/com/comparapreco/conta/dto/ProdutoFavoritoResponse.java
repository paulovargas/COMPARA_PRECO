package br.com.comparapreco.conta.dto;

import java.util.UUID;

public record ProdutoFavoritoResponse(UUID produtoId, String nome, String modelo, UUID marcaId, String marca, boolean disponivel) {}
