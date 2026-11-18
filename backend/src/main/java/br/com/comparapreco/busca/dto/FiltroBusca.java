package br.com.comparapreco.busca.dto;

import java.util.UUID;
import br.com.comparapreco.catalogo.entity.CondicaoProduto;
import br.com.comparapreco.fornecedor.entity.UnidadeFederativa;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FiltroBusca(@Size(max = 100) @Pattern(regexp = "[^\\p{Cntrl}]*") String texto,
        UUID categoriaId, UUID marcaId, @Size(max = 120) @Pattern(regexp = "[^\\p{Cntrl}]*") String modelo,
        @Min(1) Integer armazenamentoGb,
        @Size(max = 80) @Pattern(regexp = "[^\\p{Cntrl}]*") String cor, CondicaoProduto condicao,
        UUID fornecedorId, UnidadeFederativa estado,
        @Size(max = 100) @Pattern(regexp = "[^\\p{Cntrl}]*") String cidade,
        @Min(0) @Max(1000000) Integer pagina, @Min(1) @Max(50) Integer tamanho) {
    public int numeroPagina() { return pagina == null ? 0 : pagina; }
    public int tamanhoPagina() { return tamanho == null ? 20 : tamanho; }
}
