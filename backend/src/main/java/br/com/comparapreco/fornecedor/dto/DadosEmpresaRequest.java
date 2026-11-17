package br.com.comparapreco.fornecedor.dto;

import br.com.comparapreco.fornecedor.entity.UnidadeFederativa;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DadosEmpresaRequest(
        @NotBlank @Size(min = 3, max = 160) @Pattern(regexp = "[^\\p{Cntrl}]*") String razaoSocial,
        @NotBlank @Size(min = 3, max = 120) @Pattern(regexp = "[^\\p{Cntrl}]*") String nomeFantasia,
        @Size(max = 2000) @Pattern(regexp = "[^\\x00]*") String descricao,
        @NotBlank @Pattern(regexp = "[1-9][0-9]{9,14}") String whatsapp,
        @NotBlank @Size(min = 2, max = 100) @Pattern(regexp = "[^\\p{Cntrl}]*") String cidade,
        @NotNull UnidadeFederativa estado) {
    public DadosEmpresaRequest {
        razaoSocial = normalizar(razaoSocial);
        nomeFantasia = normalizar(nomeFantasia);
        cidade = normalizar(cidade);
        whatsapp = whatsapp == null ? null : whatsapp.replaceAll("[() .+-]", "");
        descricao = descricao == null ? null : descricao.strip();
    }
    private static String normalizar(String texto) {
        return texto == null ? null : texto.strip().replaceAll("(?U)\\s+", " ");
    }
}
