package br.com.comparapreco.catalogo.repository;

import java.util.UUID;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import br.com.comparapreco.catalogo.entity.VarianteProduto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VarianteProdutoRepository extends JpaRepository<VarianteProduto, UUID> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"produto.marca", "produto.categoria"})
    org.springframework.data.domain.Page<VarianteProduto> findAll(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @Query("select v from VarianteProduto v where v.id = :id")
    Optional<VarianteProduto> buscarParaOferta(UUID id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VarianteProduto v where v.id = :id")
    Optional<VarianteProduto> buscarParaEdicao(UUID id);

    @EntityGraph(attributePaths = {"produto.marca", "produto.categoria"})
    List<VarianteProduto> findAllByIdIn(Collection<UUID> ids);

    @EntityGraph(attributePaths = {"produto.marca", "produto.categoria"})
    Optional<VarianteProduto> findDetalhadaById(UUID id);
    @EntityGraph(attributePaths = {"produto.marca", "produto.categoria"})
    @Query("select v from VarianteProduto v join v.produto p where :texto = '' or locate(lower(:texto), lower(p.nome)) > 0 or locate(lower(:texto), lower(p.modelo)) > 0 or locate(lower(:texto), lower(p.marca.nome)) > 0")
    Page<VarianteProduto> buscarCatalogo(String texto, Pageable pageable);
}
