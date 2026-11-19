package br.com.comparapreco.importacao.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImportacaoResponse(UUID id, String situacao, Instant expiraEm, int criar, int atualizar,
        int ignorar, int invalidas, List<LinhaImportacao> linhas) {}
