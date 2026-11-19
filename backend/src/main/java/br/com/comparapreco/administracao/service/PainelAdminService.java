package br.com.comparapreco.administracao.service;

import java.nio.charset.StandardCharsets;
import br.com.comparapreco.administracao.dto.*;
import br.com.comparapreco.administracao.repository.PainelAdminRepository;
import br.com.comparapreco.shared.exception.ErroNegocioException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class PainelAdminService {
    private final PainelAdminRepository painel;
    public PainelAdminService(PainelAdminRepository painel) { this.painel = painel; }
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public EstatisticasResponse estatisticas() { return painel.estatisticas(); }
    @Transactional(readOnly=true) public ConfiguracaoDados configuracao() { return painel.configuracao(); }
    @Transactional public ConfiguracaoDados configurar(ConfiguracaoDados r) {
        if (r.aberturaContato().strip().length() < 3 || r.aberturaContato().codePoints().anyMatch(Character::isISOControl)) { throw new ErroNegocioException(400, "CONFIGURACAO_INVALIDA", "Informe uma abertura de contato sem caracteres de controle."); }
        if (!painel.configurar(r)) { throw new ErroNegocioException(409, "REGISTRO_DESATUALIZADO", "A configuração mudou. Recarregue antes de salvar."); }
        return painel.configuracao();
    }
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public byte[] relatorio() {
        var dados = painel.estatisticas();
        var csv = new StringBuilder("uf;fornecedores;aprovados;ofertasElegiveis\r\n");
        for (var regiao : dados.regioes()) { csv.append(regiao.uf()).append(';').append(regiao.fornecedores()).append(';').append(regiao.aprovados()).append(';').append(regiao.ofertasElegiveis()).append("\r\n"); }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}
