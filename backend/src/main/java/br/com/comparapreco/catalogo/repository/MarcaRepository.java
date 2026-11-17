package br.com.comparapreco.catalogo.repository;

import java.util.UUID;
import br.com.comparapreco.catalogo.entity.Marca;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarcaRepository extends JpaRepository<Marca, UUID> {
}
