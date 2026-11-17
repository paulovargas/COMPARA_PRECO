package br.com.comparapreco.catalogo.repository;

import java.util.UUID;
import br.com.comparapreco.catalogo.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {
}
