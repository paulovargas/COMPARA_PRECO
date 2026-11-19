package br.com.comparapreco.administracao.dto;

import java.util.UUID;
import br.com.comparapreco.catalogo.dto.SolicitacaoCatalogoResponse;
import jakarta.validation.constraints.*;

public final class SolicitacaoAdminDtos {
    private SolicitacaoAdminDtos() {}
    public record SolicitacaoAdmin(SolicitacaoCatalogoResponse solicitacao, UUID fornecedorId, String fornecedor, long versao) {}
    public enum Decisao { ATENDIDA, RECUSADA }
    public record Resolucao(@NotNull Decisao situacao, UUID varianteId, @NotBlank @Size(min=3,max=500) String motivo, @NotNull @Min(0) Long versao) {}
}
