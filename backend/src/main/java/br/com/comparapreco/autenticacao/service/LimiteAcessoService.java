package br.com.comparapreco.autenticacao.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class LimiteAcessoService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacoes;
    private final int limiteLoginIp;
    private final int limiteLoginEmail;
    private final int limiteCadastroIp;

    public LimiteAcessoService(JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
            @Value("${app.seguranca.limite-login-ip}") int limiteLoginIp,
            @Value("${app.seguranca.limite-login-email}") int limiteLoginEmail,
            @Value("${app.seguranca.limite-cadastro-ip}") int limiteCadastroIp) {
        this.jdbc = jdbc;
        this.transacoes = new TransactionTemplate(transactionManager);
        this.transacoes.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.limiteLoginIp = validarLimite(limiteLoginIp);
        this.limiteLoginEmail = validarLimite(limiteLoginEmail);
        this.limiteCadastroIp = validarLimite(limiteCadastroIp);
    }

    public void verificarLogin(String email, String ip) {
        registrar("login-ip:" + ip, limiteLoginIp, 600);
        registrar("login-email:" + email, limiteLoginEmail, 600);
    }

    public void verificarCadastro(String ip) {
        registrar("cadastro-ip:" + ip, limiteCadastroIp, 3600);
    }

    private void registrar(String identificador, int limite, int janelaSegundos) {
        // O contador é confirmado antes de lançar o erro, inclusive em requisições recusadas.
        Integer quantidade = transacoes.execute(status -> jdbc.queryForObject("""
                INSERT INTO compara_preco.limite_acesso (chave, janela_inicio, quantidade)
                VALUES (?, CURRENT_TIMESTAMP, 1)
                ON CONFLICT (chave) DO UPDATE SET
                    quantidade = CASE WHEN limite_acesso.janela_inicio <= CURRENT_TIMESTAMP - (? * INTERVAL '1 second')
                        THEN 1 ELSE LEAST(limite_acesso.quantidade, ?) + 1 END,
                    janela_inicio = CASE WHEN limite_acesso.janela_inicio <= CURRENT_TIMESTAMP - (? * INTERVAL '1 second')
                        THEN CURRENT_TIMESTAMP ELSE limite_acesso.janela_inicio END
                RETURNING quantidade
                """, Integer.class, calcularChave(identificador), janelaSegundos, limite, janelaSegundos));
        if (quantidade != null && quantidade > limite) {
            throw new LimiteAcessoException(janelaSegundos);
        }
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
    public void limparRegistrosAntigos() {
        jdbc.update("DELETE FROM compara_preco.limite_acesso WHERE janela_inicio < CURRENT_TIMESTAMP - INTERVAL '1 day'");
    }

    private int validarLimite(int limite) {
        if (limite < 1 || limite > 10000) { throw new IllegalArgumentException("Limite de acesso deve estar entre 1 e 10000."); }
        return limite;
    }

    private String calcularChave(String identificador) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identificador.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }
}
