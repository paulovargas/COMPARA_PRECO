package br.com.comparapreco.usuario.service;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;
import br.com.comparapreco.autenticacao.service.SessaoService;
import br.com.comparapreco.shared.dto.PaginaResponse;
import br.com.comparapreco.shared.exception.ErroNegocioException;
import br.com.comparapreco.usuario.dto.CadastroRequest;
import br.com.comparapreco.usuario.dto.UsuarioResponse;
import br.com.comparapreco.usuario.entity.Perfil;
import br.com.comparapreco.usuario.entity.Usuario;
import br.com.comparapreco.usuario.repository.UsuarioRepository;
import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final SessaoService sessoes;
    private final Validator validator;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                          SessaoService sessoes, Validator validator) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.sessoes = sessoes;
        this.validator = validator;
    }

    @Transactional
    public UsuarioResponse cadastrar(CadastroRequest request) {
        return UsuarioResponse.de(criar(request, Perfil.COMPRADOR));
    }

    @Transactional
    public void criarAdministradorInicial(CadastroRequest request) {
        if (!usuarios.existsByPerfil(Perfil.ADMIN)) {
            criar(request, Perfil.ADMIN);
        }
    }

    private Usuario criar(CadastroRequest request, Perfil perfil) {
        if (!validator.validate(request).isEmpty() || request.senha().getBytes(StandardCharsets.UTF_8).length > 72
                || request.nome().codePoints().anyMatch(Character::isISOControl)) {
            throw new ErroNegocioException(400, "DADOS_INVALIDOS", "Dados da conta inválidos. A senha deve ter até 72 bytes UTF-8.");
        }
        if (usuarios.existsByEmail(request.email())) {
            throw emailEmUso();
        }
        try {
            return usuarios.saveAndFlush(new Usuario(request.nome(), request.email(),
                    passwordEncoder.encode(request.senha()), perfil));
        } catch (DataIntegrityViolationException exception) {
            if (exception.getMostSpecificCause() instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                throw emailEmUso();
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public UsuarioResponse consultar(UUID id) {
        return UsuarioResponse.de(buscar(id));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> listar(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 50) {
            throw new ErroNegocioException(400, "PAGINACAO_INVALIDA", "Página inválida ou tamanho fora do intervalo de 1 a 50.");
        }
        return PaginaResponse.de(usuarios.findAll(PageRequest.of(pagina, tamanho,
                Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.asc("id")))).map(UsuarioResponse::de));
    }

    @Transactional
    public UsuarioResponse alterarSituacao(UUID id, boolean ativo, UUID administrador) {
        if (!ativo && id.equals(administrador)) {
            throw new ErroNegocioException(409, "AUTOBLOQUEIO_PROIBIDO", "Você não pode bloquear a própria conta.");
        }
        Usuario usuario = buscar(id);
        usuario.alterarSituacao(ativo);
        usuarios.flush();
        sessoes.invalidarDoUsuario(usuario.getEmail());
        return UsuarioResponse.de(usuario);
    }

    private Usuario buscar(UUID id) {
        return usuarios.findById(id).orElseThrow(() ->
                new ErroNegocioException(404, "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
    }

    private ErroNegocioException emailEmUso() {
        return new ErroNegocioException(409, "EMAIL_EM_USO", "Já existe uma conta com este email.");
    }
}
