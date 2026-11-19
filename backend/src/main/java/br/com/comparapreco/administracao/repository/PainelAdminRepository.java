package br.com.comparapreco.administracao.repository;

import java.time.Instant;
import java.util.ArrayList;
import br.com.comparapreco.administracao.dto.ConfiguracaoDados;
import br.com.comparapreco.administracao.dto.EstatisticasResponse;
import br.com.comparapreco.administracao.dto.EstatisticasResponse.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PainelAdminRepository {
    private final JdbcTemplate jdbc;
    public PainelAdminRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public ConfiguracaoDados configuracao() { return jdbc.queryForObject("SELECT abertura_contato, versao FROM compara_preco.configuracao_sistema WHERE id=1", (rs,n) -> new ConfiguracaoDados(rs.getString(1), rs.getLong(2))); }
    public boolean configurar(ConfiguracaoDados r) { return jdbc.update("UPDATE compara_preco.configuracao_sistema SET abertura_contato=?, versao=versao+1 WHERE id=1 AND versao=?", r.aberturaContato().strip(), r.versao()) == 1; }
    public EstatisticasResponse estatisticas() {
        var indicadores = new ArrayList<Indicador>();
        indicadores.add(new Indicador("Contas cadastradas", contar("SELECT count(*) FROM compara_preco.usuario")));
        indicadores.add(new Indicador("Contas ativas", contar("SELECT count(*) FROM compara_preco.usuario WHERE ativo")));
        for (String status : new String[]{"PENDENTE", "APROVADO", "BLOQUEADO"}) {
            indicadores.add(new Indicador("Fornecedores " + status, jdbc.queryForObject("SELECT count(*) FROM compara_preco.fornecedor WHERE situacao=?", Long.class, status)));
        }
        for (String status : new String[]{"PENDENTE", "APROVADA", "BLOQUEADA"}) {
            indicadores.add(new Indicador("Ofertas " + status, jdbc.queryForObject("SELECT count(*) FROM compara_preco.oferta WHERE moderacao=?", Long.class, status)));
        }
        indicadores.add(new Indicador("Ofertas elegíveis", contar("SELECT count(*) FROM compara_preco.oferta_elegivel")));
        indicadores.add(new Indicador("Produtos", contar("SELECT count(*) FROM compara_preco.produto")));
        indicadores.add(new Indicador("Variantes", contar("SELECT count(*) FROM compara_preco.variante_produto")));
        indicadores.add(new Indicador("Solicitações pendentes", contar("SELECT count(*) FROM compara_preco.solicitacao_catalogo WHERE situacao='PENDENTE'")));
        var regioes = jdbc.query("""
            SELECT f.estado, count(*) AS total, count(*) FILTER (WHERE f.situacao='APROVADO') AS aprovados,
            (SELECT count(*) FROM compara_preco.oferta_elegivel e JOIN compara_preco.fornecedor x ON x.id=e.fornecedor_id WHERE x.estado=f.estado) AS elegiveis
            FROM compara_preco.fornecedor f GROUP BY f.estado ORDER BY f.estado
            """, (rs,n) -> new Regiao(rs.getString("estado"), rs.getLong("total"), rs.getLong("aprovados"), rs.getLong("elegiveis")));
        return new EstatisticasResponse(Instant.now(), indicadores, regioes);
    }
    private long contar(String sql) { return jdbc.queryForObject(sql, Long.class); }
}
