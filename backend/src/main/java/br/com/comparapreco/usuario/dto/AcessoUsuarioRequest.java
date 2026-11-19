package br.com.comparapreco.usuario.dto;

import br.com.comparapreco.usuario.entity.Perfil;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AcessoUsuarioRequest(@NotNull Boolean ativo, @NotNull Perfil perfil, @NotNull @Min(0) Long versao) {}
