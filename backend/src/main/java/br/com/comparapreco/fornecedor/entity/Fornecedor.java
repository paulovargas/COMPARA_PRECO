package br.com.comparapreco.fornecedor.entity;

import java.time.Instant;
import java.util.UUID;
import br.com.comparapreco.shared.exception.ErroNegocioException;
import br.com.comparapreco.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "fornecedor", schema = "compara_preco")
public class Fornecedor {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
    @Column(name = "razao_social", nullable = false, length = 160)
    private String razaoSocial;
    @Column(name = "nome_fantasia", nullable = false, length = 120)
    private String nomeFantasia;
    @Column(length = 2000)
    private String descricao;
    @Column(nullable = false, length = 15)
    private String whatsapp;
    @Column(nullable = false, length = 100)
    private String cidade;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 2)
    private UnidadeFederativa estado;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private SituacaoFornecedor situacao = SituacaoFornecedor.PENDENTE;
    @Column(name = "motivo_moderacao", length = 500)
    private String motivoModeracao;
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = criadoEm;
    @Version private long versao;

    protected Fornecedor() {}

    public Fornecedor(Usuario usuario, String razaoSocial, String nomeFantasia, String descricao,
                      String whatsapp, String cidade, UnidadeFederativa estado) {
        this.usuario = usuario;
        atualizarPerfil(razaoSocial, nomeFantasia, descricao, whatsapp, cidade, estado);
    }

    public void atualizarPerfil(String razaoSocial, String nomeFantasia, String descricao,
                                String whatsapp, String cidade, UnidadeFederativa estado) {
        if (situacao == SituacaoFornecedor.BLOQUEADO) {
            throw new ErroNegocioException(403, "FORNECEDOR_BLOQUEADO", "A empresa está bloqueada para alterações.");
        }
        if (situacao == SituacaoFornecedor.APROVADO && (!razaoSocial.equalsIgnoreCase(this.razaoSocial) || !nomeFantasia.equalsIgnoreCase(this.nomeFantasia))) {
            situacao = SituacaoFornecedor.PENDENTE;
            motivoModeracao = null;
        }
        this.razaoSocial = razaoSocial;
        this.nomeFantasia = nomeFantasia;
        this.descricao = descricao;
        this.whatsapp = whatsapp;
        this.cidade = cidade;
        this.estado = estado;
        atualizadoEm = Instant.now();
    }

    public void moderar(SituacaoFornecedor destino, String motivo) {
        if (destino == situacao) { return; }
        boolean permitida = situacao == SituacaoFornecedor.PENDENTE && destino == SituacaoFornecedor.APROVADO
                || situacao != SituacaoFornecedor.BLOQUEADO && destino == SituacaoFornecedor.BLOQUEADO
                || situacao == SituacaoFornecedor.BLOQUEADO && destino == SituacaoFornecedor.PENDENTE;
        if (!permitida) { throw new ErroNegocioException(409, "TRANSICAO_INVALIDA", "Transição de fornecedor inválida."); }
        if (destino == SituacaoFornecedor.BLOQUEADO && (motivo == null || motivo.strip().length() < 3)) {
            throw new ErroNegocioException(400, "MOTIVO_OBRIGATORIO", "Informe o motivo do bloqueio.");
        }
        situacao = destino;
        motivoModeracao = destino == SituacaoFornecedor.BLOQUEADO ? motivo.strip() : null;
        atualizadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public String getCidade() {
        return cidade;
    }

    public UnidadeFederativa getEstado() {
        return estado;
    }

    public SituacaoFornecedor getSituacao() {
        return situacao;
    }

    public String getMotivoModeracao() {
        return motivoModeracao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public long getVersao() {
        return versao;
    }
}
