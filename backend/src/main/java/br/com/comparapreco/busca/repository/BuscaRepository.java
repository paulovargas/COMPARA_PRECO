package br.com.comparapreco.busca.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import br.com.comparapreco.busca.dto.FiltroBusca;
import br.com.comparapreco.busca.dto.OfertaPublicaResponse;
import br.com.comparapreco.busca.dto.OpcaoCatalogoResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BuscaRepository {
    private static final String ORIGEM = """
            FROM compara_preco.oferta_elegivel o
            JOIN compara_preco.fornecedor f ON f.id = o.fornecedor_id
            JOIN compara_preco.variante_produto v ON v.id = o.variante_id
            JOIN compara_preco.produto p ON p.id = v.produto_id
            JOIN compara_preco.marca m ON m.id = p.marca_id
            JOIN compara_preco.categoria c ON c.id = p.categoria_id
            WHERE 1=1
            """;
    private final NamedParameterJdbcTemplate jdbc;

    public BuscaRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Resumo(UUID id, BigDecimal preco, long quantidade) {}
    public record Totais(BigDecimal preco, long quantidade) {}
    private record Consulta(String sql, MapSqlParameterSource parametros) {}

    public List<Resumo> buscar(FiltroBusca filtro) {
        var q = consulta(filtro, null);
        return jdbc.query("SELECT v.id, min(o.preco) preco, count(*) quantidade " + q.sql()
                + " GROUP BY v.id ORDER BY preco, v.id LIMIT :limite OFFSET :inicio", q.parametros(),
                (rs, i) -> new Resumo(rs.getObject("id", UUID.class), rs.getBigDecimal("preco"), rs.getLong("quantidade")));
    }

    public long contarVariantes(FiltroBusca filtro) {
        var q = consulta(filtro, null);
        return jdbc.queryForObject("SELECT count(DISTINCT v.id) " + q.sql(), q.parametros(), Long.class);
    }

    public Totais totais(UUID varianteId, FiltroBusca filtro) {
        var q = consulta(filtro, varianteId);
        return jdbc.queryForObject("SELECT min(o.preco) preco, count(*) quantidade " + q.sql(), q.parametros(),
                (rs, i) -> new Totais(rs.getBigDecimal("preco"), rs.getLong("quantidade")));
    }

    public List<OfertaPublicaResponse> comparar(UUID varianteId, FiltroBusca filtro, BigDecimal minimo) {
        var q = consulta(filtro, varianteId);
        return jdbc.query("SELECT o.id, f.id fornecedor_id, f.nome_fantasia, f.cidade, f.estado, o.preco, o.estoque, o.atualizado_em "
                + q.sql() + " ORDER BY o.preco, o.id LIMIT :limite OFFSET :inicio", q.parametros(),
                (rs, i) -> new OfertaPublicaResponse(rs.getObject("id", UUID.class), rs.getObject("fornecedor_id", UUID.class),
                        rs.getString("nome_fantasia"), rs.getString("cidade"), rs.getString("estado"), rs.getBigDecimal("preco"),
                        rs.getInt("estoque"), rs.getTimestamp("atualizado_em").toInstant(),
                        minimo != null && minimo.compareTo(rs.getBigDecimal("preco")) == 0));
    }

    public List<OpcaoCatalogoResponse> categorias() { return opcoes("categoria"); }
    public List<OpcaoCatalogoResponse> marcas() { return opcoes("marca"); }

    private List<OpcaoCatalogoResponse> opcoes(String tabela) {
        // O nome da tabela vem apenas dos dois métodos acima, nunca da requisição.
        return jdbc.query("SELECT DISTINCT x.id, x.nome FROM compara_preco." + tabela + " x JOIN compara_preco.produto p ON p."
                + tabela + "_id = x.id JOIN compara_preco.variante_produto v ON v.produto_id = p.id "
                + "JOIN compara_preco.oferta_elegivel o ON o.variante_id = v.id ORDER BY x.nome, x.id",
                new MapSqlParameterSource(), (rs, i) -> new OpcaoCatalogoResponse(rs.getObject("id", UUID.class), rs.getString("nome")));
    }

    private Consulta consulta(FiltroBusca f, UUID varianteId) {
        var sql = new StringBuilder(ORIGEM);
        var p = new MapSqlParameterSource().addValue("limite", f.tamanhoPagina())
                .addValue("inicio", (long) f.numeroPagina() * f.tamanhoPagina());
        igual(sql, p, "v.id", "variante", varianteId);
        igual(sql, p, "c.id", "categoria", f.categoriaId());
        igual(sql, p, "m.id", "marca", f.marcaId());
        igual(sql, p, "v.armazenamento_gb", "armazenamento", f.armazenamentoGb());
        igual(sql, p, "v.condicao", "condicao", f.condicao() == null ? null : f.condicao().name());
        igual(sql, p, "f.id", "fornecedor", f.fornecedorId());
        igual(sql, p, "f.estado", "estado", f.estado() == null ? null : f.estado().name());
        textoExato(sql, p, "p.modelo", "modelo", f.modelo());
        textoExato(sql, p, "v.cor", "cor", f.cor());
        textoExato(sql, p, "f.cidade", "cidade", f.cidade());
        if (f.texto() != null && !f.texto().isBlank()) {
            sql.append(" AND (strpos(lower(p.nome), :texto) > 0 OR strpos(lower(p.modelo), :texto) > 0 OR strpos(lower(m.nome), :texto) > 0)");
            p.addValue("texto", f.texto().strip().toLowerCase(Locale.ROOT));
        }
        return new Consulta(sql.toString(), p);
    }

    private void textoExato(StringBuilder sql, MapSqlParameterSource p, String coluna, String chave, String valor) {
        if (valor != null && !valor.isBlank()) {
            sql.append(" AND compara_preco.normalizar_texto(").append(coluna).append(") = compara_preco.normalizar_texto(:").append(chave).append(")");
            p.addValue(chave, valor.strip());
        }
    }

    private void igual(StringBuilder sql, MapSqlParameterSource p, String coluna, String chave, Object valor) {
        if (valor != null) { sql.append(" AND ").append(coluna).append(" = :").append(chave); p.addValue(chave, valor); }
    }
}
