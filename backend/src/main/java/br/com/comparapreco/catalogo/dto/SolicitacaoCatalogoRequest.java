package br.com.comparapreco.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitacaoCatalogoRequest(
        @NotBlank @Size(min=2, max=160) @Pattern(regexp="[^\\p{Cntrl}]*") String nomeProduto,
        @NotBlank @Size(min=2, max=100) @Pattern(regexp="[^\\p{Cntrl}]*") String marca,
        @NotBlank @Size(min=2, max=120) @Pattern(regexp="[^\\p{Cntrl}]*") String modelo,
        @NotBlank @Size(min=2, max=100) @Pattern(regexp="[^\\p{Cntrl}]*") String categoria,
        @NotBlank @Size(min=10, max=2000) @Pattern(regexp="[^\\x00]*") String caracteristicas) {
    public SolicitacaoCatalogoRequest {
        nomeProduto = normalizar(nomeProduto); marca = normalizar(marca); modelo = normalizar(modelo);
        categoria = normalizar(categoria); caracteristicas = normalizar(caracteristicas);
    }
    private static String normalizar(String texto) { return texto == null ? null : texto.strip().replaceAll("(?U)\\s+", " "); }
}
