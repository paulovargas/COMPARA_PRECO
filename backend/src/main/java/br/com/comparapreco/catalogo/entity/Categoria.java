package br.com.comparapreco.catalogo.entity;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import br.com.comparapreco.catalogo.domain.AtributosVariante;
import br.com.comparapreco.catalogo.domain.TextoCatalogo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "categoria", schema = "compara_preco")
public class Categoria extends EntidadeCatalogo {

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(name = "exige_armazenamento", nullable = false)
    private boolean exigeArmazenamento;
    @Column(name = "exige_cor", nullable = false)
    private boolean exigeCor;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "atributos_obrigatorios", nullable = false, columnDefinition = "jsonb")
    private Set<String> atributosObrigatorios = Set.of();

    protected Categoria() {}

    public Categoria(String nome) {
        this(nome, false, false, Set.of());
    }

    public Categoria(String nome, boolean exigeArmazenamento, boolean exigeCor, Set<String> atributosObrigatorios) {
        this.nome = TextoCatalogo.obrigatorio(nome, "Nome", 100);
        this.exigeArmazenamento = exigeArmazenamento;
        this.exigeCor = exigeCor;
        if (atributosObrigatorios == null) { throw new IllegalArgumentException("Informe os atributos obrigatórios."); }
        Map<String, String> mapa = atributosObrigatorios.stream().collect(Collectors.toMap(chave -> chave, chave -> "obrigatorio"));
        this.atributosObrigatorios = Set.copyOf(AtributosVariante.normalizar(mapa).keySet());
    }

    public boolean isExigeArmazenamento() {
        return exigeArmazenamento;
    }

    public boolean isExigeCor() {
        return exigeCor;
    }

    public Set<String> getAtributosObrigatorios() {
        return Set.copyOf(atributosObrigatorios);
    }

    public String getNome() {
        return nome;
    }
}
