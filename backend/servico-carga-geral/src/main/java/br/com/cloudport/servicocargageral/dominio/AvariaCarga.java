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
        criadoEm = OffsetDateTime.now();
        atualizadoEm = criadoEm;
    }

    public void adicionarEvidencia(String tipo, String uri, String checksum, String responsavelEvidencia) {
        evidencias.add(new Evidencia(tipo, uri, checksum, responsavelEvidencia, OffsetDateTime.now()));
        atualizadoEm = OffsetDateTime.now();
    }

    public void iniciarInspecao(String usuario, String nota) {
        exigirAberta();
        status = StatusAvariaCarga.EM_INSPECAO;
        inspecionadoPor = usuario;
        observacoes = nota;
        atualizadoEm = OffsetDateTime.now();
    }

    public void iniciarReparo(String usuario, String nota) {
        if (status != StatusAvariaCarga.EM_INSPECAO && status != StatusAvariaCarga.BLOQUEADA) {
            throw new IllegalStateException("Avaria não está pronta para reparo.");
        }
        status = StatusAvariaCarga.EM_REPARO;
        reparadoPor = usuario;
        observacoes = nota;
        atualizadoEm = OffsetDateTime.now();
    }

    public void concluirReparo(String usuario, String nota) {
        if (status != StatusAvariaCarga.EM_REPARO && status != StatusAvariaCarga.EM_INSPECAO) {
            throw new IllegalStateException("Avaria não está em inspeção ou reparo.");
        }
        status = StatusAvariaCarga.REPARADA;
        reparadoPor = usuario;
        observacoes = nota;
        atualizadoEm = OffsetDateTime.now();
    }

    public void baixar(String usuario, String nota) {
        exigirAberta();
        status = StatusAvariaCarga.BAIXADA;
        reparadoPor = usuario;
        observacoes = nota;
        atualizadoEm = OffsetDateTime.now();
    }

    private void exigirAberta() {
        if (status == StatusAvariaCarga.REPARADA || status == StatusAvariaCarga.BAIXADA) {
            throw new IllegalStateException("Avaria já está encerrada.");
        }
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
