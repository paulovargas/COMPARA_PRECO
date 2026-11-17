package br.com.comparapreco.autenticacao.security;

import java.io.IOException;
import br.com.comparapreco.shared.exception.ErroHttpWriter;
import br.com.comparapreco.usuario.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class UsuarioAtivoFilter extends OncePerRequestFilter {
    private final UsuarioRepository usuarios;
    private final ErroHttpWriter erros;
    public UsuarioAtivoFilter(UsuarioRepository usuarios, ErroHttpWriter erros) {
        this.usuarios = usuarios;
        this.erros = erros;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UsuarioSessao principal) {
            boolean valido = usuarios.findById(principal.id()).filter(usuario -> usuario.isAtivo()
                    && usuario.getPerfil() == principal.perfil() && usuario.getEmail().equals(principal.email())).isPresent();
            if (!valido) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) { session.invalidate(); }
                erros.escrever(response, 401, "SESSAO_INVALIDA", "A sessão não está mais disponível. Entre novamente.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
