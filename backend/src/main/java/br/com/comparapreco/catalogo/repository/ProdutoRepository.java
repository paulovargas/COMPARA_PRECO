package br.com.comparapreco.catalogo.repository;

import java.util.UUID;
import br.com.comparapreco.catalogo.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, UUID> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"marca", "categoria"})
    org.springframework.data.domain.Page<Produto> findAll(org.springframework.data.domain.Pageable pageable);

}
