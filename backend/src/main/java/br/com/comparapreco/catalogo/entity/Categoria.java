package br.com.comparapreco.catalogo.entity;

import br.com.comparapreco.catalogo.domain.TextoCatalogo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "categoria", schema = "compara_preco")
public class Categoria extends EntidadeCatalogo {

    @Column(nullable = false, length = 100)
    private String nome;

    protected Categoria() {}

    public Categoria(String nome) {
        this.nome = TextoCatalogo.obrigatorio(nome, "Nome", 100);
    }

    public String getNome() {
        return nome;
    }
}
