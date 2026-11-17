package br.com.comparapreco.autenticacao.service;

import java.nio.charset.StandardCharsets;
import br.com.comparapreco.autenticacao.dto.LoginRequest;
import br.com.comparapreco.autenticacao.security.UsuarioSessao;
import br.com.comparapreco.usuario.dto.UsuarioResponse;
import br.com.comparapreco.usuario.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoService {
    private final UsuarioRepository usuarios;
    private final AuthenticationManager authenticationManager;
    private final LimiteAcessoService limites;

    public AutenticacaoService(UsuarioRepository usuarios, AuthenticationManager authenticationManager, LimiteAcessoService limites) {
        this.usuarios = usuarios;
        this.authenticationManager = authenticationManager;
        this.limites = limites;
    }

    public ResultadoLogin autenticar(LoginRequest request, String ip) {
        limites.verificarLogin(request.email(), ip);
        if (request.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Credenciais inválidas.");
        }
        authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.senha()));
        var usuario = usuarios.findByEmail(request.email()).filter(conta -> conta.isAtivo())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas."));
        return new ResultadoLogin(UsuarioSessao.de(usuario), UsuarioResponse.de(usuario));
    }

    public record ResultadoLogin(UsuarioSessao principal, UsuarioResponse usuario) {}
}
