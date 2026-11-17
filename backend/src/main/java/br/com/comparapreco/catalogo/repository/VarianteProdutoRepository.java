package br.com.comparapreco.catalogo.repository;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import br.com.comparapreco.catalogo.entity.VarianteProduto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VarianteProdutoRepository extends JpaRepository<VarianteProduto, UUID> {
    @EntityGraph(attributePaths = {"produto.marca", "produto.categoria"})
    Optional<VarianteProduto> findDetalhadaById(UUID id);
    @EntityGraph(attributePaths = {"produto.marca", "produto.categoria"})
    @Query("select v from VarianteProduto v join v.produto p where :texto = '' or locate(lower(:texto), lower(p.nome)) > 0 or locate(lower(:texto), lower(p.modelo)) > 0 or locate(lower(:texto), lower(p.marca.nome)) > 0")
    Page<VarianteProduto> buscarCatalogo(String texto, Pageable pageable);
}
