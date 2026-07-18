package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.StatusInventarioFisico;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "inventario_fisico_carga")
public class InventarioFisicoCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String codigo;

    @Column(name = "armazem_id", nullable = false, length = 80)
    private String armazemId;

    @Column(name = "posicao_referencia", length = 120)
    private String posicaoReferencia;

    @Column(nullable = false, length = 1000)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusInventarioFisico status = StatusInventarioFisico.ABERTO;

    @Column(name = "aberto_por", nullable = false, length = 120)
    private String abertoPor;

    @Column(name = "aprovado_por", length = 120)
    private String aprovadoPor;

    @Column(name = "justificativa_ajuste", length = 2000)
    private String justificativaAjuste;

    @Column(name = "historico_operacional", nullable = false, length = 16000)
    private String historicoOperacional = "";

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Column(name = "concluido_em")
    private OffsetDateTime concluidoEm;

    @Column(name = "cancelado_em")
    private OffsetDateTime canceladoEm;

    @Version
    private Long versao;

    @OneToMany(mappedBy = "inventario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("contadoEm ASC")
    private List<ContagemInventarioCarga> contagens = new ArrayList<>();

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        normalizar();
        registrarHistorico("ABERTO", abertoPor, motivo);
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
        normalizar();
    }

    public void adicionarContagem(ContagemInventarioCarga contagem) {
        if (status == StatusInventarioFisico.CONCLUIDO || status == StatusInventarioFisico.CANCELADO) {
            throw new IllegalStateException("Inventário encerrado não aceita novas contagens.");
        }
        contagem.setInventario(this);
        contagens.add(contagem);
        status = StatusInventarioFisico.EM_CONTAGEM;
        registrarHistorico("CONTAGEM", contagem.getUsuario(), contagem.getCodigoIdentificacao());
    }

    public void enviarParaAprovacao(String usuario) {
        if (contagens.isEmpty()) {
            throw new IllegalStateException("O inventário precisa de ao menos uma contagem.");
        }
        if (status == StatusInventarioFisico.CONCLUIDO || status == StatusInventarioFisico.CANCELADO) {
            throw new IllegalStateException("Inventário já está encerrado.");
        }
        status = StatusInventarioFisico.AGUARDANDO_APROVACAO;
        registrarHistorico("AGUARDANDO_APROVACAO", usuario, "Contagens encerradas.");
    }

    public void concluir(String aprovador, String justificativa) {
        if (status != StatusInventarioFisico.AGUARDANDO_APROVACAO) {
            throw new IllegalStateException("Inventário não está aguardando aprovação.");
        }
        if (aprovador == null || aprovador.isBlank() || justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("Aprovador e justificativa são obrigatórios.");
        }
        aprovadoPor = aprovador.trim();
        justificativaAjuste = justificativa.trim();
        status = StatusInventarioFisico.CONCLUIDO;
        concluidoEm = OffsetDateTime.now();
        registrarHistorico("CONCLUIDO", aprovadoPor, justificativaAjuste);
    }

    public void cancelar(String usuario, String justificativa) {
        if (status == StatusInventarioFisico.CONCLUIDO || status == StatusInventarioFisico.CANCELADO) {
            throw new IllegalStateException("Inventário já está encerrado.");
        }
        status = StatusInventarioFisico.CANCELADO;
        canceladoEm = OffsetDateTime.now();
        justificativaAjuste = justificativa;
        registrarHistorico("CANCELADO", usuario, justificativa);
    }

    public void registrarHistorico(String evento, String usuario, String detalhe) {
        String linha = OffsetDateTime.now() + "|" + normalizarUsuario(usuario) + "|" + evento + "|"
                + (detalhe == null ? "" : detalhe.trim());
        historicoOperacional = historicoOperacional == null || historicoOperacional.isBlank()
                ? linha : historicoOperacional + "\n" + linha;
    }

    private void normalizar() {
        codigo = obrigatorio(codigo).toUpperCase();
        armazemId = obrigatorio(armazemId).toUpperCase();
        motivo = obrigatorio(motivo);
        abertoPor = obrigatorio(abertoPor);
        posicaoReferencia = opcional(posicaoReferencia);
    }

    private String obrigatorio(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Campo obrigatório do inventário não informado.");
        }
        return valor.trim();
    }

    private String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toUpperCase();
    }

    private String normalizarUsuario(String usuario) {
        return usuario == null || usuario.isBlank() ? "SISTEMA" : usuario.trim();
    }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getArmazemId() { return armazemId; }
    public void setArmazemId(String armazemId) { this.armazemId = armazemId; }
    public String getPosicaoReferencia() { return posicaoReferencia; }
    public void setPosicaoReferencia(String posicaoReferencia) { this.posicaoReferencia = posicaoReferencia; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public StatusInventarioFisico getStatus() { return status; }
    public String getAbertoPor() { return abertoPor; }
    public void setAbertoPor(String abertoPor) { this.abertoPor = abertoPor; }
    public String getAprovadoPor() { return aprovadoPor; }
    public String getJustificativaAjuste() { return justificativaAjuste; }
    public String getHistoricoOperacional() { return historicoOperacional; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getConcluidoEm() { return concluidoEm; }
    public OffsetDateTime getCanceladoEm() { return canceladoEm; }
    public List<ContagemInventarioCarga> getContagens() { return Collections.unmodifiableList(contagens); }
}
