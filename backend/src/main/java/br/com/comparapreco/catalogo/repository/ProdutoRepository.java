package br.com.comparapreco.catalogo.repository;

import java.util.UUID;
import br.com.comparapreco.catalogo.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, UUID> {
}
