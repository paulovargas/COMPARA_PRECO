package br.com.comparapreco.catalogo.entity;

import java.util.Map;
import java.util.Objects;
import br.com.comparapreco.catalogo.domain.AtributosVariante;
import br.com.comparapreco.catalogo.domain.TextoCatalogo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "variante_produto", schema = "compara_preco")
public class VarianteProduto extends EntidadeCatalogo {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "armazenamento_gb")
    private Integer armazenamentoGb;

    @Column(length = 80)
    private String cor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CondicaoProduto condicao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, String> atributos;

    protected VarianteProduto() {}

    public VarianteProduto(Produto produto, Integer armazenamentoGb, String cor,
                           CondicaoProduto condicao, Map<String, String> atributos) {
        this.produto = Objects.requireNonNull(produto, "Produto é obrigatório.");
        if (armazenamentoGb != null && armazenamentoGb <= 0) {
            throw new IllegalArgumentException("Armazenamento deve ser positivo quando informado.");
        }
        this.armazenamentoGb = armazenamentoGb;
        this.cor = cor == null ? null : TextoCatalogo.normalizado(cor, "Cor", 80);
        this.condicao = Objects.requireNonNull(condicao, "Condição é obrigatória.");
        this.atributos = AtributosVariante.normalizar(atributos);
    }

    public Produto getProduto() {
        return produto;
    }

    public Integer getArmazenamentoGb() {
        return armazenamentoGb;
    }

    public String getCor() {
        return cor;
    }

    public CondicaoProduto getCondicao() {
        return condicao;
    }

    public Map<String, String> getAtributos() {
        return Map.copyOf(atributos);
    }
}
