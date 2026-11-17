package br.com.comparapreco.autenticacao.dto;

import br.com.comparapreco.usuario.entity.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
                           @NotBlank @Size(max = 64) String senha) {
    public LoginRequest {
        email = Usuario.normalizarEmail(email);
    }

    @Override
    public String toString() {
        return "LoginRequest[dados protegidos]";
    }
}
