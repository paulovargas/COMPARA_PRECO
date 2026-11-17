package br.com.comparapreco.usuario.dto;

import jakarta.validation.constraints.NotNull;

public record SituacaoUsuarioRequest(@NotNull Boolean ativo) {}
