package br.com.comparapreco.administracao.repository;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogoUsoRepository {
    private final JdbcTemplate jdbc;
    public CatalogoUsoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public boolean categoria(UUID id) { return jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM compara_preco.produto WHERE categoria_id=?)", Boolean.class, id); }
    public boolean produto(UUID id) { return jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM compara_preco.variante_produto WHERE produto_id=?)", Boolean.class, id); }
    public boolean variante(UUID id) { return jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM compara_preco.oferta WHERE variante_id=?) OR EXISTS(SELECT 1 FROM compara_preco.solicitacao_catalogo WHERE variante_id=?)", Boolean.class, id, id); }
}
