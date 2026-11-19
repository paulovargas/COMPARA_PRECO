package br.com.comparapreco.administracao.repository;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ControleAdministrativoRepository {
    private final JdbcTemplate jdbc;
    public ControleAdministrativoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void bloquearUsuarios() { jdbc.execute("SELECT pg_advisory_xact_lock(790010)"); }
    public void bloquearCatalogo() { jdbc.execute("SELECT pg_advisory_xact_lock(790011)"); }
    public boolean administradorAtivo(UUID id) {
        return jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM compara_preco.usuario WHERE id=? AND ativo AND perfil='ADMIN')", Boolean.class, id);
    }
    public boolean temEmpresa(UUID usuarioId) {
        return jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM compara_preco.fornecedor WHERE usuario_id=?)", Boolean.class, usuarioId);
    }
}
