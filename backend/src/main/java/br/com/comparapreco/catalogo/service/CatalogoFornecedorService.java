package br.com.comparapreco.catalogo.service;

import br.com.comparapreco.catalogo.dto.VarianteResponse;
import br.com.comparapreco.catalogo.repository.VarianteProdutoRepository;
import br.com.comparapreco.shared.domain.Paginacao;
import br.com.comparapreco.shared.dto.PaginaResponse;
import br.com.comparapreco.shared.exception.ErroNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogoFornecedorService {
    private final VarianteProdutoRepository variantes;
    public CatalogoFornecedorService(VarianteProdutoRepository variantes) {
        this.variantes = variantes;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<VarianteResponse> consultar(String texto, int pagina, int tamanho) {
        if (texto.length() > 100) { throw new ErroNegocioException(400, "BUSCA_INVALIDA", "Informe até 100 caracteres na busca."); }
        return PaginaResponse.de(variantes.buscarCatalogo(texto.strip(), Paginacao.criar(pagina, tamanho, "criadoEm")).map(VarianteResponse::de));
    }
}
