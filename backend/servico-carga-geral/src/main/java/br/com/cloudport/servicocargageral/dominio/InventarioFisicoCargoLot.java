package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusDivergenciaInventarioCargo;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusInventarioFisicoCargo;
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
@Table(name = "inventario_fisico_cargo_lot")
public class InventarioFisicoCargoLot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "command_id_abertura", nullable = false, unique = true)
    private UUID commandIdAbertura;

    @Column(name = "posicao", nullable = false, length = 120)
    private String posicao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusInventarioFisicoCargo status = StatusInventarioFisicoCargo.ABERTO;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "contagem_inventario_cargo_lot", joinColumns = @JoinColumn(name = "inventario_id"))
    @OrderColumn(name = "ordem")
    private List<Contagem> contagens = new ArrayList<>();

    @Column(name = "aberto_por", nullable = false, length = 120)
    private String abertoPor;

    @Column(name = "aberto_em", nullable = false)
    private OffsetDateTime abertoEm;

    @Column(name = "concluido_por", length = 120)
    private String concluidoPor;

    @Column(name = "concluido_em")
    private OffsetDateTime concluidoEm;

    @Column(name = "motivo", length = 1000)
    private String motivo;

    @PrePersist
    void prePersist() {
        abertoEm = OffsetDateTime.now();
    }

    public void registrarContagem(
            UUID commandId,
            UUID loteId,
            String identificacao,
            BigDecimal quantidadeLogica,
            BigDecimal volumeLogicoM3,
            BigDecimal pesoLogicoKg,
            BigDecimal quantidadeContada,
            BigDecimal volumeContadoM3,
            BigDecimal pesoContadoKg,
            String usuario,
            String observacao) {
        Contagem existente = contagens.stream()
                .filter(item -> item.commandId.equals(commandId))
                .findFirst()
                .orElse(null);
        if (existente != null) return;
        status = StatusInventarioFisicoCargo.EM_CONTAGEM;
        Contagem contagem = new Contagem(
                commandId,
                loteId,
                identificacao,
                quantidadeLogica,
                volumeLogicoM3,
                pesoLogicoKg,
                quantidadeContada,
                volumeContadoM3,
                pesoContadoKg,
                usuario,
                observacao,
                OffsetDateTime.now());
        contagens.add(contagem);
        status = contagem.getStatusDivergencia() == StatusDivergenciaInventarioCargo.PENDENTE
                ? StatusInventarioFisicoCargo.AGUARDANDO_APROVACAO
                : StatusInventarioFisicoCargo.EM_CONTAGEM;
    }

    public void concluir(String usuario, String motivoConclusao) {
        if (contagens.stream().anyMatch(item -> item.statusDivergencia == StatusDivergenciaInventarioCargo.PENDENTE)) {
            throw new IllegalStateException("Existem divergências pendentes de decisão.");
        }
        status = StatusInventarioFisicoCargo.CONCLUIDO;
        concluidoPor = usuario;
        concluidoEm = OffsetDateTime.now();
        motivo = motivoConclusao;
    }

    public UUID getId() { return id; }
    public UUID getCommandIdAbertura() { return commandIdAbertura; }
    public void setCommandIdAbertura(UUID commandIdAbertura) { this.commandIdAbertura = commandIdAbertura; }
    public String getPosicao() { return posicao; }
    public void setPosicao(String posicao) { this.posicao = posicao; }
    public StatusInventarioFisicoCargo getStatus() { return status; }
    public List<Contagem> getContagens() { return Collections.unmodifiableList(contagens); }
    public String getAbertoPor() { return abertoPor; }
    public void setAbertoPor(String abertoPor) { this.abertoPor = abertoPor; }
    public OffsetDateTime getAbertoEm() { return abertoEm; }
    public String getConcluidoPor() { return concluidoPor; }
    public OffsetDateTime getConcluidoEm() { return concluidoEm; }
    public String getMotivo() { return motivo; }

    @Embeddable
    public static class Contagem {

        @Column(name = "command_id", nullable = false)
        private UUID commandId;
        @Column(name = "lote_id", nullable = false)
        private UUID loteId;
        @Column(name = "identificacao", nullable = false, length = 160)
        private String identificacao;
        @Column(name = "quantidade_logica", nullable = false, precision = 19, scale = 3)
        private BigDecimal quantidadeLogica;
        @Column(name = "volume_logico_m3", nullable = false, precision = 19, scale = 3)
        private BigDecimal volumeLogicoM3;
        @Column(name = "peso_logico_kg", nullable = false, precision = 19, scale = 3)
        private BigDecimal pesoLogicoKg;
        @Column(name = "quantidade_contada", nullable = false, precision = 19, scale = 3)
        private BigDecimal quantidadeContada;
        @Column(name = "volume_contado_m3", nullable = false, precision = 19, scale = 3)
        private BigDecimal volumeContadoM3;
        @Column(name = "peso_contado_kg", nullable = false, precision = 19, scale = 3)
        private BigDecimal pesoContadoKg;
        @Enumerated(EnumType.STRING)
        @Column(name = "status_divergencia", nullable = false, length = 30)
        private StatusDivergenciaInventarioCargo statusDivergencia;
        @Column(name = "usuario", nullable = false, length = 120)
        private String usuario;
        @Column(name = "observacao", length = 1000)
        private String observacao;
        @Column(name = "contado_em", nullable = false)
        private OffsetDateTime contadoEm;
        @Column(name = "resolvido_por", length = 120)
        private String resolvidoPor;
        @Column(name = "motivo_resolucao", length = 1000)
        private String motivoResolucao;

        protected Contagem() {
        }

        public Contagem(UUID commandId, UUID loteId, String identificacao,
                BigDecimal quantidadeLogica, BigDecimal volumeLogicoM3, BigDecimal pesoLogicoKg,
                BigDecimal quantidadeContada, BigDecimal volumeContadoM3, BigDecimal pesoContadoKg,
                String usuario, String observacao, OffsetDateTime contadoEm) {
            this.commandId = commandId;
            this.loteId = loteId;
            this.identificacao = identificacao;
            this.quantidadeLogica = quantidadeLogica;
            this.volumeLogicoM3 = volumeLogicoM3;
            this.pesoLogicoKg = pesoLogicoKg;
            this.quantidadeContada = quantidadeContada;
            this.volumeContadoM3 = volumeContadoM3;
            this.pesoContadoKg = pesoContadoKg;
            this.usuario = usuario;
            this.observacao = observacao;
            this.contadoEm = contadoEm;
            this.statusDivergencia = possuiDivergencia()
                    ? StatusDivergenciaInventarioCargo.PENDENTE
                    : StatusDivergenciaInventarioCargo.SEM_DIVERGENCIA;
        }

        public void resolver(boolean ajustar, String usuarioResolucao, String motivo) {
            if (statusDivergencia != StatusDivergenciaInventarioCargo.PENDENTE) {
                throw new IllegalStateException("Divergência já foi resolvida.");
            }
            statusDivergencia = ajustar
                    ? StatusDivergenciaInventarioCargo.AJUSTADA
                    : StatusDivergenciaInventarioCargo.REJEITADA;
            resolvidoPor = usuarioResolucao;
            motivoResolucao = motivo;
        }

        private boolean possuiDivergencia() {
            return quantidadeLogica.compareTo(quantidadeContada) != 0
                    || volumeLogicoM3.compareTo(volumeContadoM3) != 0
                    || pesoLogicoKg.compareTo(pesoContadoKg) != 0;
        }

        public UUID getCommandId() { return commandId; }
        public UUID getLoteId() { return loteId; }
        public String getIdentificacao() { return identificacao; }
        public BigDecimal getQuantidadeLogica() { return quantidadeLogica; }
        public BigDecimal getVolumeLogicoM3() { return volumeLogicoM3; }
        public BigDecimal getPesoLogicoKg() { return pesoLogicoKg; }
        public BigDecimal getQuantidadeContada() { return quantidadeContada; }
        public BigDecimal getVolumeContadoM3() { return volumeContadoM3; }
        public BigDecimal getPesoContadoKg() { return pesoContadoKg; }
        public StatusDivergenciaInventarioCargo getStatusDivergencia() { return statusDivergencia; }
        public String getUsuario() { return usuario; }
        public String getObservacao() { return observacao; }
        public OffsetDateTime getContadoEm() { return contadoEm; }
        public String getResolvidoPor() { return resolvidoPor; }
        public String getMotivoResolucao() { return motivoResolucao; }
    }
}
