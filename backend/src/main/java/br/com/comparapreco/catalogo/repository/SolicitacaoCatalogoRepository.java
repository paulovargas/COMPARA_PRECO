package br.com.comparapreco.catalogo.repository;

import br.com.comparapreco.administracao.dto.SolicitacaoAdminDtos.SolicitacaoAdmin;
import br.com.comparapreco.shared.dto.PaginaResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import br.com.comparapreco.catalogo.dto.SolicitacaoCatalogoRequest;
import br.com.comparapreco.catalogo.dto.SolicitacaoCatalogoResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SolicitacaoCatalogoRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public SolicitacaoCatalogoRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    public long contarPendentes(UUID fornecedorId) {
        return jdbc.queryForObject("SELECT count(*) FROM compara_preco.solicitacao_catalogo WHERE fornecedor_id=:fornecedor AND situacao='PENDENTE'",
                new MapSqlParameterSource("fornecedor", fornecedorId), Long.class);
    }
    public boolean pendenteIgual(UUID fornecedorId, SolicitacaoCatalogoRequest request) {
        return jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM compara_preco.solicitacao_catalogo WHERE fornecedor_id=:fornecedor AND situacao='PENDENTE'
                AND compara_preco.normalizar_texto(nome_produto)=compara_preco.normalizar_texto(:nome)
                AND compara_preco.normalizar_texto(marca)=compara_preco.normalizar_texto(:marca)
                AND compara_preco.normalizar_texto(modelo)=compara_preco.normalizar_texto(:modelo)
                AND compara_preco.normalizar_texto(categoria)=compara_preco.normalizar_texto(:categoria)
                AND compara_preco.normalizar_texto(caracteristicas)=compara_preco.normalizar_texto(:caracteristicas))
                """, parametros(fornecedorId, request), Boolean.class);
    }
    public SolicitacaoCatalogoResponse criar(UUID fornecedorId, SolicitacaoCatalogoRequest request) {
        return jdbc.queryForObject("""
                INSERT INTO compara_preco.solicitacao_catalogo (id, fornecedor_id, nome_produto, marca, modelo, categoria, caracteristicas)
                VALUES (:id, :fornecedor, :nome, :marca, :modelo, :categoria, :caracteristicas) RETURNING *
                """, parametros(fornecedorId, request).addValue("id", UUID.randomUUID()), this::mapear);
    }
    public long contarDoUsuario(UUID usuarioId) {
        return jdbc.queryForObject("SELECT count(*) FROM compara_preco.solicitacao_catalogo s JOIN compara_preco.fornecedor f ON f.id=s.fornecedor_id WHERE f.usuario_id=:usuario",
                new MapSqlParameterSource("usuario", usuarioId), Long.class);
    }
    public List<SolicitacaoCatalogoResponse> listar(UUID usuarioId, int pagina, int tamanho) {
        return jdbc.query("""
                SELECT s.* FROM compara_preco.solicitacao_catalogo s JOIN compara_preco.fornecedor f ON f.id=s.fornecedor_id
                WHERE f.usuario_id=:usuario ORDER BY s.criado_em DESC, s.id LIMIT :tamanho OFFSET :inicio
                """, new MapSqlParameterSource("usuario", usuarioId).addValue("tamanho", tamanho).addValue("inicio", (long) pagina * tamanho), this::mapear);
    }
    public PaginaResponse<SolicitacaoAdmin> listarAdministracao(String situacao, int pagina, int tamanho) {
        var params = new MapSqlParameterSource().addValue("situacao", situacao, java.sql.Types.VARCHAR).addValue("tamanho", tamanho).addValue("inicio", (long) pagina * tamanho);
        var itens = jdbc.query("SELECT s.*, f.nome_fantasia FROM compara_preco.solicitacao_catalogo s JOIN compara_preco.fornecedor f ON f.id=s.fornecedor_id WHERE (:situacao IS NULL OR s.situacao=:situacao) ORDER BY s.criado_em DESC, s.id LIMIT :tamanho OFFSET :inicio",
                params, (rs, n) -> new SolicitacaoAdmin(mapear(rs, n), rs.getObject("fornecedor_id", UUID.class), rs.getString("nome_fantasia"), rs.getLong("versao")));
        long total = jdbc.queryForObject("SELECT count(*) FROM compara_preco.solicitacao_catalogo WHERE (:situacao IS NULL OR situacao=:situacao)", params, Long.class);
        return new PaginaResponse<>(itens, pagina, tamanho, total, (int) ((total + tamanho - 1) / tamanho));
    }
    public int resolver(UUID id, String situacao, UUID varianteId, String motivo, long versao) {
        return jdbc.update("UPDATE compara_preco.solicitacao_catalogo SET situacao=:situacao, variante_id=:variante, motivo=:motivo, versao=versao+1, atualizado_em=current_timestamp WHERE id=:id AND versao=:versao AND situacao='PENDENTE'",
                new MapSqlParameterSource("id", id).addValue("situacao", situacao).addValue("variante", varianteId).addValue("motivo", motivo).addValue("versao", versao));
    }
    private MapSqlParameterSource parametros(UUID fornecedorId, SolicitacaoCatalogoRequest r) {
        return new MapSqlParameterSource("fornecedor", fornecedorId).addValue("nome", r.nomeProduto()).addValue("marca", r.marca())
                .addValue("modelo", r.modelo()).addValue("categoria", r.categoria()).addValue("caracteristicas", r.caracteristicas());
    }
    private SolicitacaoCatalogoResponse mapear(ResultSet rs, int linha) throws SQLException {
        return new SolicitacaoCatalogoResponse(rs.getObject("id", UUID.class), rs.getString("nome_produto"), rs.getString("marca"),
                rs.getString("modelo"), rs.getString("categoria"), rs.getString("caracteristicas"), rs.getString("situacao"),
                rs.getString("motivo"), rs.getObject("variante_id", UUID.class), rs.getTimestamp("criado_em").toInstant());
    }
}
