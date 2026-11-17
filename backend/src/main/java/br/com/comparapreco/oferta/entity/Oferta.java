package br.com.comparapreco.oferta.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import br.com.comparapreco.catalogo.entity.VarianteProduto;
import br.com.comparapreco.fornecedor.entity.Fornecedor;
import br.com.comparapreco.shared.exception.ErroNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "oferta", schema = "compara_preco")
public class Oferta {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "variante_id", nullable = false)
    private VarianteProduto variante;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;
    @Column(nullable = false) private int estoque;
    @Column(nullable = false) private boolean disponivel;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ModeracaoOferta moderacao = ModeracaoOferta.PENDENTE;
    @Column(name = "motivo_moderacao", length = 500) private String motivoModeracao;
    @Column(name = "criado_em", nullable = false, updatable = false) private Instant criadoEm = Instant.now();
    @Column(name = "atualizado_em", nullable = false) private Instant atualizadoEm = criadoEm;
    @Version private long versao;

    protected Oferta() {}
    public Oferta(Fornecedor fornecedor, VarianteProduto variante, BigDecimal preco, int estoque, boolean disponivel) {
        this.fornecedor = fornecedor;
        this.variante = variante;
        atualizar(preco, estoque, disponivel);
    }

    public void atualizar(BigDecimal preco, int estoque, boolean disponivel) {
        if (preco == null || preco.signum() <= 0 || preco.scale() > 2 || preco.compareTo(new BigDecimal("9999999999.99")) > 0
                || estoque < 0 || estoque > 1000000000) {
            throw new ErroNegocioException(400, "OFERTA_INVALIDA", "Preço ou estoque inválido.");
        }
        this.preco = preco.setScale(2, RoundingMode.UNNECESSARY);
        this.estoque = estoque;
        this.disponivel = disponivel;
        atualizadoEm = Instant.now();
    }

    public void moderar(ModeracaoOferta destino, String motivo) {
        if (destino == moderacao) { return; }
        boolean permitida = moderacao == ModeracaoOferta.PENDENTE && destino == ModeracaoOferta.APROVADA
                || moderacao != ModeracaoOferta.BLOQUEADA && destino == ModeracaoOferta.BLOQUEADA
                || moderacao == ModeracaoOferta.BLOQUEADA && destino == ModeracaoOferta.PENDENTE;
        if (!permitida) { throw new ErroNegocioException(409, "TRANSICAO_INVALIDA", "Transição de moderação inválida."); }
        if (destino == ModeracaoOferta.BLOQUEADA && (motivo == null || motivo.strip().length() < 3)) {
            throw new ErroNegocioException(400, "MOTIVO_OBRIGATORIO", "Informe o motivo do bloqueio.");
        }
        moderacao = destino;
        motivoModeracao = destino == ModeracaoOferta.BLOQUEADA ? motivo.strip() : null;
        atualizadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public VarianteProduto getVariante() {
        return variante;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getEstoque() {
        return estoque;
    }

    public boolean isDisponivel() {
        return disponivel;
    }

    public ModeracaoOferta getModeracao() {
        return moderacao;
    }

    public String getMotivoModeracao() {
        return motivoModeracao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public long getVersao() {
        return versao;
    }
}
