package br.com.comparapreco.contato.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import br.com.comparapreco.contato.dto.ContatoResponse;
import br.com.comparapreco.contato.repository.ContatoRepository;
import br.com.comparapreco.oferta.repository.OfertaRepository;
import br.com.comparapreco.shared.exception.ErroNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContatoService {
    private final ContatoRepository contatos;
    private final OfertaRepository ofertas;
    private final br.com.comparapreco.administracao.repository.PainelAdminRepository configuracoes;
    public ContatoService(ContatoRepository contatos, OfertaRepository ofertas, br.com.comparapreco.administracao.repository.PainelAdminRepository configuracoes) { this.contatos = contatos; this.ofertas = ofertas; this.configuracoes = configuracoes; }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ContatoResponse consultar(UUID id) {
        if (!contatos.elegivel(id)) { throw new ErroNegocioException(404, "CONTATO_INDISPONIVEL", "Esta oferta não está mais disponível para contato."); }
        var oferta = ofertas.findById(id).orElseThrow();
        var v = oferta.getVariante();
        var p = v.getProduto();
        var mensagem = new StringBuilder(configuracoes.configuracao().aberturaContato()).append(" ")
                .append(p.getNome()).append(" — ").append(p.getMarca().getNome()).append(" / ").append(p.getModelo())
                .append(". Condição: ").append(v.getCondicao());
        if (v.getArmazenamentoGb() != null) { mensagem.append(". Armazenamento: ").append(v.getArmazenamentoGb()).append(" GB"); }
        if (v.getCor() != null) { mensagem.append(". Cor: ").append(v.getCor()); }
        v.getAtributos().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .forEach(a -> mensagem.append(". ").append(a.getKey()).append(": ").append(a.getValue()));
        mensagem.append(". A oferta ainda está disponível?");
        return new ContatoResponse("https://wa.me/" + oferta.getFornecedor().getWhatsapp() + "?text="
                + URLEncoder.encode(mensagem.toString(), StandardCharsets.UTF_8));
    }
}
