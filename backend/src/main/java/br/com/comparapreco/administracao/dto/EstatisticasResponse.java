package br.com.comparapreco.administracao.dto;

import java.time.Instant;
import java.util.List;

public record EstatisticasResponse(Instant geradoEm, List<Indicador> indicadores, List<Regiao> regioes) {
    public record Indicador(String nome, long quantidade) {}
    public record Regiao(String uf, long fornecedores, long aprovados, long ofertasElegiveis) {}
}
