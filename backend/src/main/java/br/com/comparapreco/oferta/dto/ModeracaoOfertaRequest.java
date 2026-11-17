package br.com.comparapreco.oferta.dto;

import br.com.comparapreco.oferta.entity.ModeracaoOferta;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ModeracaoOfertaRequest(@NotNull ModeracaoOferta moderacao, @Size(max = 500) @Pattern(regexp = "[^\\x00]*") String motivo,
                                     @NotNull @PositiveOrZero Long versao) {}
