package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusAvariaCarga;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "avaria_carga")
public class AvariaCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "command_id", nullable = false, unique = true)
    private UUID commandId;

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Column(name = "codigo", nullable = false, length = 80)
    private String codigo;

    @Column(name = "descricao", nullable = false, length = 1000)
    private String descricao;

    @Column(name = "quantidade_afetada", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidadeAfetada;

    @Column(name = "volume_afetado_m3", nullable = false, precision = 19, scale = 3)
    private BigDecimal volumeAfetadoM3;

    @Column(name = "peso_afetado_kg", nullable = false, precision = 19, scale = 3)
    private BigDecimal pesoAfetadoKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusAvariaCarga status = StatusAvariaCarga.BLOQUEADA;

    @Column(name = "responsavel", nullable = false, length = 120)
    private String responsavel;

    @Column(name = "inspecionado_por", length = 120)
    private String inspecionadoPor;

    @Column(name = "reparado_por", length = 120)
    private String reparadoPor;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "evidencia_avaria_carga", joinColumns = @JoinColumn(name = "avaria_id"))
    @OrderColumn(name = "ordem")
    private List<Evidencia> evidencias = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        normalizarCampos();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
        normalizarCampos();
    }

    public void adicionarEvidencia(String tipo, String uri, String checksum, String responsavelEvidencia) {
        exigirNaoEncerrada();
        evidencias.add(new Evidencia(
                normalizarCodigo(tipo),
                normalizarTexto(uri),
                normalizarTextoOpcional(checksum),
                normalizarTexto(responsavelEvidencia),
                OffsetDateTime.now()));
        atualizadoEm = OffsetDateTime.now();
    }

    public void iniciarInspecao(String usuario, String nota) {
        exigirStatus(StatusAvariaCarga.BLOQUEADA, "Avaria deve estar bloqueada para iniciar inspeção.");
        status = StatusAvariaCarga.EM_INSPECAO;
        inspecionadoPor = normalizarTexto(usuario);
        observacoes = normalizarTextoOpcional(nota);
        atualizadoEm = OffsetDateTime.now();
    }

    public void iniciarReparo(String usuario, String nota) {
        exigirStatus(StatusAvariaCarga.EM_INSPECAO, "Avaria deve estar em inspeção para iniciar reparo.");
        status = StatusAvariaCarga.EM_REPARO;
        reparadoPor = normalizarTexto(usuario);
        observacoes = normalizarTextoOpcional(nota);
        atualizadoEm = OffsetDateTime.now();
    }

    public void concluirReparo(String usuario, String nota) {
        exigirStatus(StatusAvariaCarga.EM_REPARO, "Avaria deve estar em reparo para ser concluída.");
        status = StatusAvariaCarga.REPARADA;
        reparadoPor = normalizarTexto(usuario);
        observacoes = normalizarTextoOpcional(nota);
        atualizadoEm = OffsetDateTime.now();
    }

    public void baixar(String usuario, String nota) {
        exigirNaoEncerrada();
        status = StatusAvariaCarga.BAIXADA;
        reparadoPor = normalizarTexto(usuario);
        observacoes = normalizarTextoOpcional(nota);
        atualizadoEm = OffsetDateTime.now();
    }

    private void exigirStatus(StatusAvariaCarga esperado, String mensagem) {
        if (status != esperado) {
            throw new IllegalStateException(mensagem);
        }
    }

    private void exigirNaoEncerrada() {
        if (status == StatusAvariaCarga.REPARADA || status == StatusAvariaCarga.BAIXADA) {
            throw new IllegalStateException("Avaria já está encerrada.");
        }
    }

    private void normalizarCampos() {
        codigo = normalizarCodigo(codigo);
        descricao = normalizarTexto(descricao);
        responsavel = normalizarTexto(responsavel);
        inspecionadoPor = normalizarTextoOpcional(inspecionadoPor);
        reparadoPor = normalizarTextoOpcional(reparadoPor);
        observacoes = normalizarTextoOpcional(observacoes);
    }

    private String normalizarCodigo(String valor) {
        return normalizarTexto(valor).toUpperCase();
    }

    private String normalizarTexto(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String normalizarTextoOpcional(String valor) {
        String normalizado = normalizarTexto(valor);
        return normalizado == null || normalizado.isEmpty() ? null : normalizado;
    }

    public UUID getId() { return id; }
    public UUID getCommandId() { return commandId; }
    public void setCommandId(UUID commandId) { this.commandId = commandId; }
    public UUID getLoteId() { return loteId; }
    public void setLoteId(UUID loteId) { this.loteId = loteId; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getQuantidadeAfetada() { return quantidadeAfetada; }
    public void setQuantidadeAfetada(BigDecimal quantidadeAfetada) { this.quantidadeAfetada = quantidadeAfetada; }
    public BigDecimal getVolumeAfetadoM3() { return volumeAfetadoM3; }
    public void setVolumeAfetadoM3(BigDecimal volumeAfetadoM3) { this.volumeAfetadoM3 = volumeAfetadoM3; }
    public BigDecimal getPesoAfetadoKg() { return pesoAfetadoKg; }
    public void setPesoAfetadoKg(BigDecimal pesoAfetadoKg) { this.pesoAfetadoKg = pesoAfetadoKg; }
    public StatusAvariaCarga getStatus() { return status; }
    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }
    public String getInspecionadoPor() { return inspecionadoPor; }
    public String getReparadoPor() { return reparadoPor; }
    public String getObservacoes() { return observacoes; }
    public List<Evidencia> getEvidencias() { return Collections.unmodifiableList(evidencias); }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }

    @Embeddable
    public static class Evidencia {
        @Column(name = "tipo", nullable = false, length = 40)
        private String tipo;
        @Column(name = "uri", nullable = false, length = 1000)
        private String uri;
        @Column(name = "checksum", length = 128)
        private String checksum;
        @Column(name = "responsavel", nullable = false, length = 120)
        private String responsavel;
        @Column(name = "registrado_em", nullable = false)
        private OffsetDateTime registradoEm;

        protected Evidencia() {
        }

        public Evidencia(String tipo, String uri, String checksum, String responsavel, OffsetDateTime registradoEm) {
            this.tipo = tipo;
            this.uri = uri;
            this.checksum = checksum;
            this.responsavel = responsavel;
            this.registradoEm = registradoEm;
        }

        public String getTipo() { return tipo; }
        public String getUri() { return uri; }
        public String getChecksum() { return checksum; }
        public String getResponsavel() { return responsavel; }
        public OffsetDateTime getRegistradoEm() { return registradoEm; }
    }
}
