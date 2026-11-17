package br.com.comparapreco.catalogo.entity;

import java.net.URI;
import java.util.Objects;
import br.com.comparapreco.catalogo.domain.TextoCatalogo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "produto", schema = "compara_preco")
public class Produto extends EntidadeCatalogo {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marca_id", nullable = false)
    private Marca marca;

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(nullable = false, length = 120)
    private String modelo;

    @Column(length = 2000)
    private String descricao;

    @Column(name = "imagem_url", length = 2000)
    private String imagemUrl;

    protected Produto() {}

    public Produto(Categoria categoria, Marca marca, String nome, String modelo) {
        this(categoria, marca, nome, modelo, null, null);
    }

    public Produto(Categoria categoria, Marca marca, String nome, String modelo,
                   String descricao, String imagemUrl) {
        this.categoria = Objects.requireNonNull(categoria, "Categoria é obrigatória.");
        this.marca = Objects.requireNonNull(marca, "Marca é obrigatória.");
        this.nome = TextoCatalogo.obrigatorio(nome, "Nome", 160);
        this.modelo = TextoCatalogo.obrigatorio(modelo, "Modelo", 120);
        this.descricao = descricao == null ? null : TextoCatalogo.obrigatorio(descricao, "Descrição", 2000);
        if (imagemUrl != null) {
            URI uri = URI.create(TextoCatalogo.obrigatorio(imagemUrl, "Imagem", 2000));
            if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost() == null) {
                throw new IllegalArgumentException("Imagem deve ser uma URL HTTP ou HTTPS válida.");
            }
            this.imagemUrl = uri.toString();
        }
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Marca getMarca() {
        return marca;
    }

    public String getNome() {
        return nome;
    }

    public String getModelo() {
        return modelo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }
}
