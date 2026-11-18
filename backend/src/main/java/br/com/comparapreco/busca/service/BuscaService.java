package br.com.comparapreco.busca.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import br.com.comparapreco.busca.dto.ComparacaoResponse;
import br.com.comparapreco.busca.dto.FiltroBusca;
import br.com.comparapreco.busca.dto.OpcaoCatalogoResponse;
import br.com.comparapreco.busca.dto.VarianteBuscaResponse;
import br.com.comparapreco.busca.repository.BuscaRepository;
import br.com.comparapreco.catalogo.dto.VarianteResponse;
import br.com.comparapreco.catalogo.entity.VarianteProduto;
import br.com.comparapreco.catalogo.domain.ValidacaoVarianteOferta;
import br.com.comparapreco.catalogo.repository.VarianteProdutoRepository;
import br.com.comparapreco.shared.dto.PaginaResponse;
import br.com.comparapreco.shared.exception.ErroNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class BuscaService {
    private final BuscaRepository busca;
    private final VarianteProdutoRepository variantes;
    public BuscaService(BuscaRepository busca, VarianteProdutoRepository variantes) {
        this.busca = busca; this.variantes = variantes;
    }

    public PaginaResponse<VarianteBuscaResponse> buscar(FiltroBusca filtro) {
        long total = busca.contarVariantes(filtro);
        var resumos = busca.buscar(filtro);
        Map<UUID, VarianteProduto> catalogo = resumos.isEmpty() ? Map.of() : variantes
                .findAllByIdIn(resumos.stream().map(BuscaRepository.Resumo::id).toList())
                .stream().collect(Collectors.toMap(VarianteProduto::getId, Function.identity()));
        var itens = resumos.stream().map(r -> {
            var v = catalogo.get(r.id());
            return new VarianteBuscaResponse(VarianteResponse.de(v), v.getProduto().getImagemUrl(), r.preco(), r.quantidade());
        }).toList();
        return pagina(itens, filtro, total);
    }

    public ComparacaoResponse comparar(UUID id, FiltroBusca filtro) {
        var variante = variantes.findDetalhadaById(id).filter(v -> ValidacaoVarianteOferta.pendencias(v).isEmpty())
                .orElseThrow(() -> new ErroNegocioException(404, "VARIANTE_NAO_ENCONTRADA", "Variante não encontrada."));
        var totais = busca.totais(id, filtro);
        return new ComparacaoResponse(VarianteResponse.de(variante), totais.preco(),
                pagina(busca.comparar(id, filtro, totais.preco()), filtro, totais.quantidade()));
    }

    public List<OpcaoCatalogoResponse> categorias() { return busca.categorias(); }
    public List<OpcaoCatalogoResponse> marcas() { return busca.marcas(); }

    private <T> PaginaResponse<T> pagina(List<T> itens, FiltroBusca filtro, long total) {
        return new PaginaResponse<>(itens, filtro.numeroPagina(), filtro.tamanhoPagina(), total,
                (int) ((total + filtro.tamanhoPagina() - 1) / filtro.tamanhoPagina()));
    }
}
