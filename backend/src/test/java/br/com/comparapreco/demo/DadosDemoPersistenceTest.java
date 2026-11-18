package br.com.comparapreco.demo;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import br.com.comparapreco.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DadosDemoPersistenceTest extends PostgresIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder encoder;

    @Test
    void deveCarregarExemplosComEmpateEOfertasInelegiveisSemDuplicar() {
        carregar(); carregar();
        assertThat(contar("usuario", "50000000")).isEqualTo(6);
        assertThat(contar("fornecedor", "60000000")).isEqualTo(5);
        assertThat(contar("oferta", "70000000")).isEqualTo(20);
        assertThat(contar("oferta_elegivel", "70000000")).isEqualTo(14);
        assertThat(contar("solicitacao_catalogo", "80000000")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM compara_preco.usuario WHERE id::text LIKE '50000000-%' AND perfil='ADMIN'", Integer.class)).isZero();
        var precos = jdbc.queryForList("SELECT preco FROM compara_preco.oferta_elegivel WHERE variante_id='40000000-0000-0000-0000-000000000001' ORDER BY preco,id", BigDecimal.class);
        assertThat(precos).containsExactly(new BigDecimal("6200.00"), new BigDecimal("6200.00"), new BigDecimal("6390.00"));
        String hash = jdbc.queryForObject("SELECT senha_hash FROM compara_preco.usuario WHERE id='50000000-0000-0000-0000-000000000001'", String.class);
        assertThat(encoder.matches("ComparaDemo2026!", hash)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM compara_preco.produto_favorito WHERE usuario_id='50000000-0000-0000-0000-000000000001'", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM compara_preco.fornecedor_seguido WHERE usuario_id='50000000-0000-0000-0000-000000000001'", Integer.class)).isEqualTo(2);
    }

    @Test
    void devePreservarDadosEditadosAoReaplicarCarga() {
        carregar();
        jdbc.update("UPDATE compara_preco.usuario SET nome='Comprador alterado' WHERE id='50000000-0000-0000-0000-000000000001'");
        jdbc.update("UPDATE compara_preco.oferta SET preco=6100.00, estoque=0 WHERE id='70000000-0000-0000-0000-000000000001'");
        carregar();
        assertThat(jdbc.queryForObject("SELECT nome FROM compara_preco.usuario WHERE id='50000000-0000-0000-0000-000000000001'", String.class)).isEqualTo("Comprador alterado");
        assertThat(jdbc.queryForObject("SELECT preco FROM compara_preco.oferta WHERE id='70000000-0000-0000-0000-000000000001'", BigDecimal.class)).isEqualByComparingTo("6100.00");
        assertThat(jdbc.queryForObject("SELECT estoque FROM compara_preco.oferta WHERE id='70000000-0000-0000-0000-000000000001'", Integer.class)).isZero();
        assertThat(contar("oferta", "70000000")).isEqualTo(20);
    }

    private void carregar() {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            for (String nome : new String[]{"R__catalogo_demo.sql", "R__ofertas_e_contas_demo.sql"}) {
                ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("db/demo/" + nome), StandardCharsets.UTF_8));
            }
            return null;
        });
    }
    private int contar(String tabela, String prefixo) {
        return jdbc.queryForObject("SELECT count(*) FROM compara_preco." + tabela + " WHERE id::text LIKE ?", Integer.class, prefixo + "-%");
    }
}
