package br.com.comparapreco.autenticacao.service;

import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

@Service
public class SessaoService {
    private final FindByIndexNameSessionRepository<? extends Session> sessoes;

    public SessaoService(FindByIndexNameSessionRepository<? extends Session> sessoes) {
        this.sessoes = sessoes;
    }

    public void invalidarDoUsuario(String email) {
        sessoes.findByPrincipalName(email).keySet().forEach(sessoes::deleteById);
    }
}
