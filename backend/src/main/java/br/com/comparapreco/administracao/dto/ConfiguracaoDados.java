package br.com.comparapreco.administracao.dto;

import jakarta.validation.constraints.*;

public record ConfiguracaoDados(@NotBlank @Size(min=3,max=160) String aberturaContato, @NotNull @Min(0) Long versao) {}
