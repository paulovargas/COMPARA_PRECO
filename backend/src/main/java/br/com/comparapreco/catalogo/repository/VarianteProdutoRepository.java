package br.com.comparapreco.catalogo.repository;

import java.util.UUID;
import br.com.comparapreco.catalogo.entity.VarianteProduto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VarianteProdutoRepository extends JpaRepository<VarianteProduto, UUID> {
}
