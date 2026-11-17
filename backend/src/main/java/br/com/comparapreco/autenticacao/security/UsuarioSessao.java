package br.com.comparapreco.autenticacao.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import br.com.comparapreco.usuario.entity.Perfil;
import br.com.comparapreco.usuario.entity.Usuario;
import org.springframework.security.core.AuthenticatedPrincipal;

public record UsuarioSessao(UUID id, String nome, String email, Perfil perfil)
        implements AuthenticatedPrincipal, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @Override
    public String getName() {
        return email;
    }

    public static UsuarioSessao de(Usuario usuario) {
        return new UsuarioSessao(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil());
    }
}
