package br.com.comparapreco.usuario.service;

import java.util.UUID;
import br.com.comparapreco.administracao.repository.ControleAdministrativoRepository;
import br.com.comparapreco.autenticacao.service.SessaoService;
import br.com.comparapreco.shared.domain.ControleVersao;
import br.com.comparapreco.shared.exception.ErroNegocioException;
import br.com.comparapreco.usuario.dto.UsuarioResponse;
import br.com.comparapreco.usuario.entity.Perfil;
import br.com.comparapreco.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissoesService {
    private final UsuarioRepository usuarios;
    private final SessaoService sessoes;
    private final ControleAdministrativoRepository controle;
    public PermissoesService(UsuarioRepository usuarios, SessaoService sessoes, ControleAdministrativoRepository controle) {
        this.usuarios = usuarios; this.sessoes = sessoes; this.controle = controle;
    }
    @Transactional
    public UsuarioResponse alterar(UUID id, boolean ativo, Perfil perfil, Long versao, UUID administrador) {
        // Serializa alterações de acesso e revalida o autor após aguardar outro administrador.
        controle.bloquearUsuarios();
        if (!controle.administradorAtivo(administrador)) { throw new ErroNegocioException(403, "ACESSO_NEGADO", "Administrador não está mais ativo."); }
        var usuario = usuarios.buscarParaCadastroFornecedor(id).orElseThrow(() -> new ErroNegocioException(404, "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
        if (versao != null) { ControleVersao.exigirAtual(versao, usuario.getVersao()); }
        Perfil destino = perfil == null ? usuario.getPerfil() : perfil;
        if (id.equals(administrador) && (!ativo || destino != Perfil.ADMIN)) {
            throw new ErroNegocioException(409, "AUTOBLOQUEIO_PROIBIDO", "Você não pode bloquear ou remover o próprio acesso administrativo.");
        }
        if (destino == Perfil.FORNECEDOR && usuario.getPerfil() != Perfil.FORNECEDOR && !controle.temEmpresa(id)) {
            throw new ErroNegocioException(409, "EMPRESA_OBRIGATORIA", "A conta precisa cadastrar sua empresa antes de receber o perfil fornecedor.");
        }
        boolean mudou = usuario.isAtivo() != ativo || usuario.getPerfil() != destino;
        usuario.alterarSituacao(ativo); usuario.alterarPerfil(destino); usuarios.flush();
        if (mudou) { sessoes.invalidarDoUsuario(usuario.getEmail()); }
        return UsuarioResponse.de(usuario);
    }
}
