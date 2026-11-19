package br.com.comparapreco.administracao.dto;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import br.com.comparapreco.catalogo.entity.CondicaoProduto;
import jakarta.validation.constraints.*;

public final class CatalogoAdminDtos {
    private CatalogoAdminDtos() {}
    public record CategoriaDados(UUID id, @NotBlank @Size(max=100) String nome, @NotNull Boolean exigeArmazenamento,
            @NotNull Boolean exigeCor, @NotNull @Size(max=8) Set<@NotBlank @Size(max=32) String> atributosObrigatorios, @NotNull @Min(0) Long versao) {}
    public record MarcaDados(UUID id, @NotBlank @Size(max=100) String nome, @NotNull @Min(0) Long versao) {}
    public record ProdutoDados(UUID id, @NotNull UUID categoriaId, @NotNull UUID marcaId, @NotBlank @Size(max=160) String nome,
            @NotBlank @Size(max=120) String modelo, @Size(max=2000) String descricao, @Size(max=2000) String imagemUrl, @NotNull @Min(0) Long versao) {}
    public record VarianteDados(UUID id, @NotNull UUID produtoId, @Positive Integer armazenamentoGb, @Size(max=80) String cor,
            @NotNull CondicaoProduto condicao, @NotNull @Size(max=8) Map<@NotBlank @Size(max=32) String, @NotBlank @Size(max=64) String> atributos, @NotNull @Min(0) Long versao, String nomeProduto, String modelo, String marca) {}
}
