package br.com.comparapreco.usuario.dto;

import br.com.comparapreco.usuario.entity.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CadastroRequest(
        @NotBlank @Size(min = 2, max = 100) String nome,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 64) String senha) {
    public CadastroRequest {
        nome = nome == null ? null : nome.strip().replaceAll("[ ]+", " ");
        email = Usuario.normalizarEmail(email);
    }

    @Override
    public String toString() {
        return "CadastroRequest[dados protegidos]";
    }
}
