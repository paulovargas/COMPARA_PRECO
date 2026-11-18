package br.com.comparapreco.conta.repository;

import java.util.List;
import java.util.UUID;
import br.com.comparapreco.conta.dto.FornecedorSeguidoResponse;
import br.com.comparapreco.conta.dto.ProdutoFavoritoResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ColecaoRepository {
    public enum Tipo {
        PRODUTO("produto_favorito", "produto_id"), FORNECEDOR("fornecedor_seguido", "fornecedor_id");
        final String tabela;
        final String coluna;
        Tipo(String tabela, String coluna) { this.tabela = tabela; this.coluna = coluna; }
    }
    private static final String FORNECEDOR_PUBLICO = "f.situacao='APROVADO' AND u.ativo AND u.perfil='FORNECEDOR'";
    private final NamedParameterJdbcTemplate jdbc;
    public ColecaoRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean salvo(UUID usuario, UUID recurso, Tipo tipo) {
        return jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM compara_preco." + tipo.tabela
                + " WHERE usuario_id=:usuario AND " + tipo.coluna + "=:recurso)", parametros(usuario, recurso), Boolean.class);
    }

    public int salvar(UUID usuario, UUID recurso, Tipo tipo) {
        String elegivel = tipo == Tipo.PRODUTO
                ? "EXISTS (SELECT 1 FROM compara_preco.oferta_elegivel o JOIN compara_preco.variante_produto v ON v.id=o.variante_id WHERE v.produto_id=:recurso)"
                : "EXISTS (SELECT 1 FROM compara_preco.fornecedor f JOIN compara_preco.usuario u ON u.id=f.usuario_id WHERE f.id=:recurso AND " + FORNECEDOR_PUBLICO + ")";
        return jdbc.update("INSERT INTO compara_preco." + tipo.tabela + " (usuario_id, " + tipo.coluna
                + ") SELECT :usuario, :recurso WHERE " + elegivel + " ON CONFLICT DO NOTHING", parametros(usuario, recurso));
    }

    public void remover(UUID usuario, UUID recurso, Tipo tipo) {
        jdbc.update("DELETE FROM compara_preco." + tipo.tabela + " WHERE usuario_id=:usuario AND " + tipo.coluna + "=:recurso", parametros(usuario, recurso));
    }

    public long contar(UUID usuario, Tipo tipo) {
        return jdbc.queryForObject("SELECT count(*) FROM compara_preco." + tipo.tabela + " WHERE usuario_id=:usuario", parametros(usuario, null), Long.class);
    }

    public List<ProdutoFavoritoResponse> produtos(UUID usuario, int pagina, int tamanho) {
        return jdbc.query("""
                SELECT p.id, p.nome, p.modelo, m.id marca_id, m.nome marca,
                    EXISTS (SELECT 1 FROM compara_preco.oferta_elegivel o JOIN compara_preco.variante_produto v ON v.id=o.variante_id WHERE v.produto_id=p.id) disponivel
                FROM compara_preco.produto_favorito s JOIN compara_preco.produto p ON p.id=s.produto_id
                JOIN compara_preco.marca m ON m.id=p.marca_id
                WHERE s.usuario_id=:usuario ORDER BY s.criado_em DESC, s.produto_id LIMIT :tamanho OFFSET :inicio
                """, pagina(usuario, pagina, tamanho), (rs, i) -> new ProdutoFavoritoResponse(rs.getObject("id", UUID.class),
                rs.getString("nome"), rs.getString("modelo"), rs.getObject("marca_id", UUID.class), rs.getString("marca"), rs.getBoolean("disponivel")));
    }

    public List<FornecedorSeguidoResponse> fornecedores(UUID usuario, int pagina, int tamanho) {
        return jdbc.query("SELECT f.id, f.nome_fantasia, f.cidade, f.estado, (" + FORNECEDOR_PUBLICO + ") disponivel "
                + "FROM compara_preco.fornecedor_seguido s JOIN compara_preco.fornecedor f ON f.id=s.fornecedor_id "
                + "JOIN compara_preco.usuario u ON u.id=f.usuario_id WHERE s.usuario_id=:usuario "
                + "ORDER BY s.criado_em DESC, s.fornecedor_id LIMIT :tamanho OFFSET :inicio", pagina(usuario, pagina, tamanho),
                (rs, i) -> new FornecedorSeguidoResponse(rs.getObject("id", UUID.class), rs.getString("nome_fantasia"),
                        rs.getString("cidade"), rs.getString("estado"), rs.getBoolean("disponivel")));
    }

    private MapSqlParameterSource parametros(UUID usuario, UUID recurso) {
        return new MapSqlParameterSource().addValue("usuario", usuario).addValue("recurso", recurso);
    }
    private MapSqlParameterSource pagina(UUID usuario, int pagina, int tamanho) {
        return parametros(usuario, null).addValue("tamanho", tamanho).addValue("inicio", (long) pagina * tamanho);
    }
}
