package br.com.comparapreco.importacao.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import br.com.comparapreco.catalogo.domain.ValidacaoVarianteOferta;
import br.com.comparapreco.catalogo.dto.VarianteResponse;
import br.com.comparapreco.catalogo.repository.VarianteProdutoRepository;
import br.com.comparapreco.fornecedor.service.FornecedorService;
import br.com.comparapreco.importacao.dto.ImportacaoResponse;
import br.com.comparapreco.importacao.dto.LinhaImportacao;
import br.com.comparapreco.oferta.entity.Oferta;
import br.com.comparapreco.oferta.repository.OfertaRepository;
import br.com.comparapreco.shared.exception.ErroNegocioException;
import br.com.comparapreco.importacao.repository.ImportacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportacaoService {
    private final FornecedorService fornecedores;
    private final OfertaRepository ofertas;
    private final VarianteProdutoRepository variantes;
    private final LeitorOfertas leitor;
    private final ImportacaoRepository importacoes;

    public ImportacaoService(FornecedorService fornecedores, OfertaRepository ofertas, VarianteProdutoRepository variantes,
            LeitorOfertas leitor, ImportacaoRepository importacoes) {
        this.fornecedores = fornecedores;
        this.ofertas = ofertas;
        this.variantes = variantes;
        this.leitor = leitor;
        this.importacoes = importacoes;
    }

    @Transactional
    public ImportacaoResponse preparar(UUID usuarioId, MultipartFile arquivo) {
        var empresa = fornecedores.buscarAprovadoParaOperacao(usuarioId);
        var dados = leitor.ler(arquivo);
        var agora = Instant.now();
        importacoes.limparExpiradas(empresa.getId(), agora);
        int pendentes = importacoes.contarPendentes(empresa.getId());
        if (pendentes >= 10) { throw new ErroNegocioException(409, "LIMITE_IMPORTACOES", "Há 10 prévias pendentes. Aguarde a expiração de 30 minutos."); }
        var ocorrencias = new HashMap<String, Integer>();
        for (var campos : dados) {
            if (!campos.isEmpty()) { ocorrencias.merge(campos.getFirst().toLowerCase(java.util.Locale.ROOT), 1, Integer::sum); }
        }
        var linhas = new ArrayList<LinhaImportacao>();
        for (int i = 0; i < dados.size(); i++) { linhas.add(validar(i + 2, dados.get(i), empresa.getId(), ocorrencias)); }
        var previa = resposta(UUID.randomUUID(), "PREVIA", agora.plus(30, ChronoUnit.MINUTES), linhas);
        importacoes.salvar(empresa.getId(), agora, previa);
        return previa;
    }

    private LinhaImportacao validar(int numero, List<String> campos, UUID empresaId, HashMap<String, Integer> ocorrencias) {
        var erros = new ArrayList<String>();
        if (campos.size() != 4) { erros.add("Informe as quatro colunas do modelo."); }
        String codigo = campo(campos, 0), preco = campo(campos, 1), estoque = campo(campos, 2), disponivel = campo(campos, 3);
        UUID id = null;
        try {
            id = UUID.fromString(codigo);
            if (!id.toString().equalsIgnoreCase(codigo)) { throw new IllegalArgumentException(); }
        } catch (IllegalArgumentException e) { erros.add("Código da variante inválido."); }
        if (ocorrencias.getOrDefault(codigo.toLowerCase(java.util.Locale.ROOT), 0) > 1) { erros.add("Variante repetida neste arquivo."); }
        BigDecimal valor = null;
        if (!preco.matches("(0|[1-9][0-9]{0,9})([,.][0-9]{1,2})?")) { erros.add("Preço deve ser decimal com até duas casas, sem milhares."); }
        else {
            valor = new BigDecimal(preco.replace(',', '.'));
            if (valor.signum() <= 0 || valor.compareTo(new BigDecimal("9999999999.99")) > 0) { erros.add("Preço deve ser maior que zero e até 9999999999,99."); }
        }
        if (!estoque.matches("(0|[1-9][0-9]{0,9})") || new BigDecimal(estoque).compareTo(new BigDecimal("1000000000")) > 0) {
            erros.add("Estoque deve ser inteiro entre 0 e 1000000000.");
        }
        if (!disponivel.equals("true") && !disponivel.equals("false")) { erros.add("Disponibilidade deve ser true ou false."); }
        VarianteResponse variante = null;
        Oferta oferta = null;
        if (id != null) {
            var entidade = variantes.findDetalhadaById(id).orElse(null);
            if (entidade == null) { erros.add("Variante não encontrada no catálogo."); }
            else {
                variante = VarianteResponse.de(entidade);
                if (!variante.completa()) { erros.add("Variante incompleta. Solicite revisão do catálogo."); }
                oferta = ofertas.findByFornecedorIdAndVarianteId(empresaId, id).orElse(null);
            }
        }
        String acao = "INVALIDA";
        if (erros.isEmpty()) {
            acao = oferta == null ? "CRIAR" : iguais(oferta, valor, Integer.parseInt(estoque), Boolean.parseBoolean(disponivel)) ? "IGNORAR" : "ATUALIZAR";
        }
        return new LinhaImportacao(numero, codigo, preco, estoque, disponivel, variante, acao,
                oferta == null ? null : oferta.getId(), oferta == null ? null : oferta.getVersao(), List.copyOf(erros));
    }

    @Transactional(readOnly = true)
    public ImportacaoResponse consultar(UUID usuarioId, UUID id) {
        return carregar(usuarioId, id, false);
    }

    @Transactional
    public ImportacaoResponse confirmar(UUID usuarioId, UUID id) {
        // A empresa é sempre bloqueada primeiro, como no CRUD e na moderação de ofertas.
        var empresa = fornecedores.buscarAprovadoParaOperacao(usuarioId);
        var previa = carregar(usuarioId, id, true);
        if (previa.situacao().equals("APLICADA")) { return previa; }
        if (previa.invalidas() > 0) { throw new ErroNegocioException(409, "IMPORTACAO_INVALIDA", "Corrija as linhas e envie uma nova prévia antes de confirmar."); }
        for (var linha : previa.linhas()) {
            var variante = variantes.findDetalhadaById(UUID.fromString(linha.varianteId())).orElseThrow(ImportacaoService::conflito);
            ValidacaoVarianteOferta.exigirCompleta(variante);
            var atual = ofertas.findByFornecedorIdAndVarianteId(empresa.getId(), variante.getId()).orElse(null);
            if (linha.ofertaId() == null) {
                if (atual != null) { throw conflito(); }
                ofertas.save(new Oferta(empresa, variante, valor(linha), Integer.parseInt(linha.estoque()), Boolean.parseBoolean(linha.disponivel())));
            } else {
                if (atual == null || !atual.getId().equals(linha.ofertaId()) || atual.getVersao() != linha.versao()) { throw conflito(); }
                if (!linha.acao().equals("IGNORAR")) {
                    atual.atualizar(valor(linha), Integer.parseInt(linha.estoque()), Boolean.parseBoolean(linha.disponivel()));
                }
            }
        }
        ofertas.flush();
        var resultado = resposta(id, "APLICADA", previa.expiraEm(), previa.linhas());
        importacoes.marcarAplicada(resultado);
        return resultado;
    }

    private ImportacaoResponse carregar(UUID usuarioId, UUID id, boolean bloquear) {
        var previa = importacoes.buscarDoUsuario(usuarioId, id, bloquear).orElseThrow(() ->
                new ErroNegocioException(404, "IMPORTACAO_NAO_ENCONTRADA", "Importa??o n?o encontrada."));
        if (previa.situacao().equals("PREVIA") && !previa.expiraEm().isAfter(Instant.now())) {
            throw new ErroNegocioException(410, "IMPORTACAO_EXPIRADA", "A prévia expirou. Envie o arquivo novamente.");
        }
        return previa;
    }

    private static ImportacaoResponse resposta(UUID id, String situacao, Instant expiraEm, List<LinhaImportacao> linhas) {
        return new ImportacaoResponse(id, situacao, expiraEm, contar(linhas, "CRIAR"), contar(linhas, "ATUALIZAR"),
                contar(linhas, "IGNORAR"), contar(linhas, "INVALIDA"), List.copyOf(linhas));
    }
    private static int contar(List<LinhaImportacao> linhas, String acao) { return (int) linhas.stream().filter(l -> l.acao().equals(acao)).count(); }
    private static String campo(List<String> campos, int i) { return campos.size() > i ? campos.get(i) : ""; }
    private static BigDecimal valor(LinhaImportacao linha) { return new BigDecimal(linha.preco().replace(',', '.')); }
    private static boolean iguais(Oferta oferta, BigDecimal preco, int estoque, boolean disponivel) {
        return oferta.getPreco().compareTo(preco) == 0 && oferta.getEstoque() == estoque && oferta.isDisponivel() == disponivel;
    }
    private static ErroNegocioException conflito() {
        return new ErroNegocioException(409, "IMPORTACAO_DESATUALIZADA", "Uma oferta mudou desde a prévia. Envie o arquivo novamente para revisar.");
    }
}
