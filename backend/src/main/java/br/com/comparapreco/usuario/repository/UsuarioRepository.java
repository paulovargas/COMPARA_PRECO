package br.com.comparapreco.usuario.repository;

import java.util.Optional;
import java.util.UUID;
import br.com.comparapreco.usuario.entity.Perfil;
import br.com.comparapreco.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPerfil(Perfil perfil);
}
