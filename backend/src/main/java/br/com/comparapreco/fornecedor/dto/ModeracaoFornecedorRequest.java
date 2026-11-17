package br.com.comparapreco.fornecedor.dto;

import br.com.comparapreco.fornecedor.entity.SituacaoFornecedor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ModeracaoFornecedorRequest(@NotNull SituacaoFornecedor situacao, @Size(max = 500) @Pattern(regexp = "[^\\x00]*") String motivo,
                                         @NotNull @PositiveOrZero Long versao) {}
