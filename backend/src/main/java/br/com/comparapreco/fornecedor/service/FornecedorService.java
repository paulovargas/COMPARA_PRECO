package br.com.comparapreco.fornecedor.service;

import java.util.UUID;
import br.com.comparapreco.autenticacao.service.SessaoService;
import br.com.comparapreco.fornecedor.dto.AtualizarFornecedorRequest;
import br.com.comparapreco.fornecedor.dto.DadosEmpresaRequest;
import br.com.comparapreco.fornecedor.dto.FornecedorResponse;
import br.com.comparapreco.fornecedor.dto.ModeracaoFornecedorRequest;
import br.com.comparapreco.fornecedor.entity.Fornecedor;
import br.com.comparapreco.fornecedor.entity.SituacaoFornecedor;
import br.com.comparapreco.fornecedor.repository.FornecedorRepository;
import br.com.comparapreco.shared.domain.ControleVersao;
import br.com.comparapreco.shared.domain.Paginacao;
import br.com.comparapreco.shared.dto.PaginaResponse;
import br.com.comparapreco.shared.exception.ErroNegocioException;
import br.com.comparapreco.usuario.entity.Perfil;
import br.com.comparapreco.usuario.repository.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FornecedorService {
    private final FornecedorRepository fornecedores;
    private final UsuarioRepository usuarios;
    private final SessaoService sessoes;
    public FornecedorService(FornecedorRepository fornecedores, UsuarioRepository usuarios, SessaoService sessoes) {
        this.fornecedores = fornecedores; this.usuarios = usuarios; this.sessoes = sessoes;
    }

    @Transactional
    public FornecedorResponse cadastrar(UUID usuarioId, DadosEmpresaRequest dados) {
        var usuario = usuarios.buscarParaCadastroFornecedor(usuarioId).orElseThrow(() -> naoEncontrado());
        if (fornecedores.existsByUsuarioId(usuarioId)) {
            throw new ErroNegocioException(409, "FORNECEDOR_JA_CADASTRADO", "Esta conta já possui uma empresa cadastrada.");
        }
        if (!usuario.isAtivo() || usuario.getPerfil() != Perfil.COMPRADOR) {
            throw new ErroNegocioException(403, "CADASTRO_NAO_PERMITIDO", "Esta conta não pode cadastrar uma empresa.");
        }
        var fornecedor = fornecedores.saveAndFlush(new Fornecedor(usuario, dados.razaoSocial(), dados.nomeFantasia(),
                dados.descricao(), dados.whatsapp(), dados.cidade(), dados.estado()));
        usuario.tornarFornecedor();
        usuarios.flush();
        sessoes.invalidarDoUsuario(usuario.getEmail());
        return FornecedorResponse.de(fornecedor);
    }

    @Transactional(readOnly = true)
    public FornecedorResponse consultarProprio(UUID usuarioId) {
        return FornecedorResponse.de(fornecedores.findByUsuarioId(usuarioId).orElseThrow(() -> naoEncontrado()));
    }

    @Transactional
    public FornecedorResponse atualizar(UUID usuarioId, AtualizarFornecedorRequest request) {
        var fornecedor = fornecedores.buscarDoUsuarioParaAlteracao(usuarioId).orElseThrow(() -> naoEncontrado());
        ControleVersao.exigirAtual(request.versao(), fornecedor.getVersao());
        var dados = request.empresa();
        fornecedor.atualizarPerfil(dados.razaoSocial(), dados.nomeFantasia(), dados.descricao(), dados.whatsapp(), dados.cidade(), dados.estado());
        fornecedores.flush();
        return FornecedorResponse.de(fornecedor);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PaginaResponse<FornecedorResponse> listarAdministracao(SituacaoFornecedor situacao, int pagina, int tamanho) {
        var pageable = Paginacao.criar(pagina, tamanho, "criadoEm");
        var resultado = situacao == null ? fornecedores.findAll(pageable) : fornecedores.findBySituacao(situacao, pageable);
        return PaginaResponse.de(resultado.map(FornecedorResponse::de));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public FornecedorResponse moderar(UUID id, ModeracaoFornecedorRequest request) {
        var fornecedor = fornecedores.buscarParaAlteracao(id).orElseThrow(() -> naoEncontrado());
        ControleVersao.exigirAtual(request.versao(), fornecedor.getVersao());
        if (request.situacao() == SituacaoFornecedor.APROVADO && (!fornecedor.getUsuario().isAtivo()
                || fornecedor.getUsuario().getPerfil() != Perfil.FORNECEDOR)) {
            throw new ErroNegocioException(409, "CONTA_INDISPONIVEL", "A conta da empresa não está disponível para aprovação.");
        }
        fornecedor.moderar(request.situacao(), request.motivo());
        fornecedores.flush();
        return FornecedorResponse.de(fornecedor);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Fornecedor buscarAprovadoParaOperacao(UUID usuarioId) {
        var fornecedor = fornecedores.buscarDoUsuarioParaAlteracao(usuarioId).orElseThrow(() -> naoEncontrado());
        exigirAprovado(fornecedor);
        return fornecedor;
    }

    public static void exigirAprovado(Fornecedor fornecedor) {
        if (fornecedor.getSituacao() == SituacaoFornecedor.BLOQUEADO) {
            throw new ErroNegocioException(403, "FORNECEDOR_BLOQUEADO", "A empresa está bloqueada para alterações.");
        }
        if (fornecedor.getSituacao() != SituacaoFornecedor.APROVADO || !fornecedor.getUsuario().isAtivo()
                || fornecedor.getUsuario().getPerfil() != Perfil.FORNECEDOR) {
            throw new ErroNegocioException(409, "FORNECEDOR_NAO_APROVADO", "A empresa precisa estar aprovada e com conta ativa.");
        }
    }

    private ErroNegocioException naoEncontrado() {
        return new ErroNegocioException(404, "FORNECEDOR_NAO_ENCONTRADO", "Fornecedor não encontrado.");
    }
}
