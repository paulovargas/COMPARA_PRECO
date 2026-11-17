package br.com.comparapreco.fornecedor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AtualizarFornecedorRequest(@NotNull @Valid DadosEmpresaRequest empresa, @NotNull @PositiveOrZero Long versao) {}
