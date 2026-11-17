package br.com.comparapreco.oferta.service;

import java.util.UUID;
import br.com.comparapreco.catalogo.domain.ValidacaoVarianteOferta;
import br.com.comparapreco.catalogo.repository.VarianteProdutoRepository;
import br.com.comparapreco.fornecedor.repository.FornecedorRepository;
import br.com.comparapreco.fornecedor.service.FornecedorService;
import br.com.comparapreco.oferta.dto.AtualizarOfertaRequest;
import br.com.comparapreco.oferta.dto.CriarOfertaRequest;
import br.com.comparapreco.oferta.dto.ModeracaoOfertaRequest;
import br.com.comparapreco.oferta.dto.OfertaResponse;
import br.com.comparapreco.oferta.entity.ModeracaoOferta;
import br.com.comparapreco.oferta.entity.Oferta;
import br.com.comparapreco.oferta.repository.OfertaRepository;
import br.com.comparapreco.shared.domain.ControleVersao;
import br.com.comparapreco.shared.domain.Paginacao;
import br.com.comparapreco.shared.dto.PaginaResponse;
import br.com.comparapreco.shared.exception.ErroNegocioException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfertaService {
    private final OfertaRepository ofertas;
    private final VarianteProdutoRepository variantes;
    private final FornecedorService fornecedores;
    private final FornecedorRepository empresas;
    public OfertaService(OfertaRepository ofertas, VarianteProdutoRepository variantes, FornecedorService fornecedores, FornecedorRepository empresas) {
        this.ofertas = ofertas; this.variantes = variantes; this.fornecedores = fornecedores; this.empresas = empresas;
    }

    @Transactional
    public OfertaResponse criar(UUID usuarioId, CriarOfertaRequest request) {
        var fornecedor = fornecedores.buscarAprovadoParaOperacao(usuarioId);
        var variante = variantes.findDetalhadaById(request.varianteId()).orElseThrow(() ->
                new ErroNegocioException(404, "VARIANTE_NAO_ENCONTRADA", "Variante não encontrada."));
        ValidacaoVarianteOferta.exigirCompleta(variante);
        if (ofertas.existsByFornecedorIdAndVarianteId(fornecedor.getId(), variante.getId())) {
            throw new ErroNegocioException(409, "OFERTA_JA_CADASTRADA", "Esta empresa já possui oferta para a variante.");
        }
        return OfertaResponse.de(ofertas.saveAndFlush(new Oferta(fornecedor, variante, request.preco(), request.estoque(), request.disponivel())));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<OfertaResponse> listarProprias(UUID usuarioId, int pagina, int tamanho) {
        return PaginaResponse.de(ofertas.findByFornecedorUsuarioId(usuarioId, Paginacao.criar(pagina, tamanho, "atualizadoEm")).map(OfertaResponse::de));
    }

    @Transactional(readOnly = true)
    public OfertaResponse consultarPropria(UUID usuarioId, UUID id) {
        return OfertaResponse.de(ofertas.findByIdAndFornecedorUsuarioId(id, usuarioId).orElseThrow(() -> naoEncontrada()));
    }

    @Transactional
    public OfertaResponse atualizar(UUID usuarioId, UUID id, AtualizarOfertaRequest request) {
        fornecedores.buscarAprovadoParaOperacao(usuarioId);
        var oferta = ofertas.buscarDoUsuarioParaAlteracao(id, usuarioId).orElseThrow(() -> naoEncontrada());
        ControleVersao.exigirAtual(request.versao(), oferta.getVersao());
        ValidacaoVarianteOferta.exigirCompleta(oferta.getVariante());
        oferta.atualizar(request.preco(), request.estoque(), request.disponivel());
        ofertas.flush();
        return OfertaResponse.de(oferta);
    }

    @Transactional
    public void excluir(UUID usuarioId, UUID id, long versao) {
        fornecedores.buscarAprovadoParaOperacao(usuarioId);
        var oferta = ofertas.buscarDoUsuarioParaAlteracao(id, usuarioId).orElseThrow(() -> naoEncontrada());
        ControleVersao.exigirAtual(versao, oferta.getVersao());
        ofertas.delete(oferta);
        ofertas.flush();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PaginaResponse<OfertaResponse> listarAdministracao(ModeracaoOferta moderacao, UUID fornecedorId, int pagina, int tamanho) {
        return PaginaResponse.de(ofertas.buscarAdministracao(moderacao, fornecedorId, Paginacao.criar(pagina, tamanho, "atualizadoEm")).map(OfertaResponse::de));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public OfertaResponse moderar(UUID id, ModeracaoOfertaRequest request) {
        // Mantém a ordem de bloqueios: empresa antes da oferta, também usada na edição pelo proprietário.
        UUID empresaId = ofertas.buscarFornecedorId(id).orElseThrow(() -> naoEncontrada());
        var empresa = empresas.buscarParaAlteracao(empresaId).orElseThrow(() -> naoEncontrada());
        var oferta = ofertas.buscarParaAlteracao(id).orElseThrow(() -> naoEncontrada());
        ControleVersao.exigirAtual(request.versao(), oferta.getVersao());
        if (request.moderacao() == ModeracaoOferta.APROVADA) {
            FornecedorService.exigirAprovado(empresa);
            ValidacaoVarianteOferta.exigirCompleta(oferta.getVariante());
        }
        oferta.moderar(request.moderacao(), request.motivo());
        ofertas.flush();
        return OfertaResponse.de(oferta);
    }

    private ErroNegocioException naoEncontrada() {
        return new ErroNegocioException(404, "OFERTA_NAO_ENCONTRADA", "Oferta não encontrada.");
    }
}
