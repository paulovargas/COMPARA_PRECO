package br.com.comparapreco.oferta.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import br.com.comparapreco.oferta.entity.ModeracaoOferta;
import br.com.comparapreco.oferta.entity.Oferta;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OfertaRepository extends JpaRepository<Oferta, UUID> {
    Optional<Oferta> findByFornecedorIdAndVarianteId(UUID fornecedorId, UUID varianteId);
    boolean existsByFornecedorIdAndVarianteId(UUID fornecedorId, UUID varianteId);
    @EntityGraph(attributePaths = {"fornecedor", "variante.produto.marca", "variante.produto.categoria"})
    Page<Oferta> findByFornecedorUsuarioId(UUID usuarioId, Pageable pageable);
    @EntityGraph(attributePaths = {"fornecedor", "variante.produto.marca", "variante.produto.categoria"})
    Optional<Oferta> findByIdAndFornecedorUsuarioId(UUID id, UUID usuarioId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Oferta o where o.id = :id and o.fornecedor.usuario.id = :usuarioId")
    Optional<Oferta> buscarDoUsuarioParaAlteracao(UUID id, UUID usuarioId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Oferta o where o.id = :id")
    Optional<Oferta> buscarParaAlteracao(UUID id);
    @Query("select o.fornecedor.id from Oferta o where o.id = :id")
    Optional<UUID> buscarFornecedorId(UUID id);
    @EntityGraph(attributePaths = {"fornecedor", "variante.produto.marca", "variante.produto.categoria"})
    @Query("select o from Oferta o where (:moderacao is null or o.moderacao = :moderacao) and (:fornecedorId is null or o.fornecedor.id = :fornecedorId)")
    Page<Oferta> buscarAdministracao(ModeracaoOferta moderacao, UUID fornecedorId, Pageable pageable);
    @Query(value = "SELECT * FROM compara_preco.oferta_elegivel WHERE variante_id = :varianteId ORDER BY preco, id", nativeQuery = true)
    List<Oferta> buscarElegiveis(UUID varianteId);
}
