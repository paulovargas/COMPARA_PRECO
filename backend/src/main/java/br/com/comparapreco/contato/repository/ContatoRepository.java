package br.com.comparapreco.contato.repository;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ContatoRepository {
    private final JdbcTemplate jdbc;
    public ContatoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public boolean elegivel(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM compara_preco.oferta_elegivel WHERE id=?)", Boolean.class, id));
    }
}
