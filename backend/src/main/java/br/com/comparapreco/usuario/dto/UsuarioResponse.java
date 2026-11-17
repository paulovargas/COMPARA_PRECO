package br.com.comparapreco.usuario.dto;

import java.time.Instant;
import java.util.UUID;
import br.com.comparapreco.usuario.entity.Perfil;
import br.com.comparapreco.usuario.entity.Usuario;

public record UsuarioResponse(UUID id, String nome, String email, Perfil perfil, boolean ativo, Instant criadoEm) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(),
                usuario.isAtivo(), usuario.getCriadoEm());
    }
}
