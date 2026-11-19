package br.com.comparapreco.importacao.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import br.com.comparapreco.importacao.dto.ImportacaoResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
public class ImportacaoRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public ImportacaoRepository(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }

    public void limparExpiradas(UUID fornecedorId, Instant agora) {
        jdbc.update("DELETE FROM compara_preco.importacao_ofertas WHERE fornecedor_id = ? AND expira_em < ?", fornecedorId, Timestamp.from(agora));
    }
    public int contarPendentes(UUID fornecedorId) {
        return jdbc.queryForObject("SELECT count(*) FROM compara_preco.importacao_ofertas WHERE fornecedor_id = ? AND NOT aplicada", Integer.class, fornecedorId);
    }
    public void salvar(UUID fornecedorId, Instant criadoEm, ImportacaoResponse previa) {
        jdbc.update("INSERT INTO compara_preco.importacao_ofertas(id, fornecedor_id, criado_em, expira_em, previa) VALUES (?, ?, ?, ?, ?)",
                previa.id(), fornecedorId, Timestamp.from(criadoEm), Timestamp.from(previa.expiraEm()), mapper.writeValueAsString(previa));
    }
    public Optional<ImportacaoResponse> buscarDoUsuario(UUID usuarioId, UUID id, boolean bloquear) {
        var resultados = jdbc.query("SELECT i.previa FROM compara_preco.importacao_ofertas i JOIN compara_preco.fornecedor f ON f.id = i.fornecedor_id "
                + "WHERE i.id = ? AND f.usuario_id = ?" + (bloquear ? " FOR UPDATE OF i" : ""),
                (rs, n) -> mapper.readValue(rs.getString(1), ImportacaoResponse.class), id, usuarioId);
        return resultados.stream().findFirst();
    }
    public void marcarAplicada(ImportacaoResponse resultado) {
        jdbc.update("UPDATE compara_preco.importacao_ofertas SET aplicada = TRUE, previa = ? WHERE id = ?", mapper.writeValueAsString(resultado), resultado.id());
    }
}
