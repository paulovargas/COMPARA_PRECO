package br.com.comparapreco.conta.dto;

import java.util.UUID;

public record FornecedorSeguidoResponse(UUID fornecedorId, String nome, String cidade, String estado, boolean disponivel) {}
