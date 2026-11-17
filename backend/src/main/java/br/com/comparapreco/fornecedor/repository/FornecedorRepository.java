package br.com.comparapreco.fornecedor.repository;

import java.util.Optional;
import java.util.UUID;
import br.com.comparapreco.fornecedor.entity.Fornecedor;
import br.com.comparapreco.fornecedor.entity.SituacaoFornecedor;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface FornecedorRepository extends JpaRepository<Fornecedor, UUID> {
    boolean existsByUsuarioId(UUID usuarioId);
    Optional<Fornecedor> findByUsuarioId(UUID usuarioId);
    Page<Fornecedor> findBySituacao(SituacaoFornecedor situacao, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Fornecedor f where f.usuario.id = :usuarioId")
    Optional<Fornecedor> buscarDoUsuarioParaAlteracao(UUID usuarioId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Fornecedor f where f.id = :id")
    Optional<Fornecedor> buscarParaAlteracao(UUID id);
}
