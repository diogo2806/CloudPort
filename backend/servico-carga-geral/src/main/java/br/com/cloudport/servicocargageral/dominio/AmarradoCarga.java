package br.com.cloudport.servicocargageral.dominio;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "amarrado_carga", uniqueConstraints = {
        @UniqueConstraint(name = "uk_amarrado_carga_codigo", columnNames = "codigo")
})
public class AmarradoCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String codigo;

    @Column(name = "visita_navio_id", nullable = false, length = 80)
    private String visitaNavioId;

    @Column(nullable = false)
    private boolean integro = true;

    @Column(name = "destino_direcionamento", nullable = false, length = 100)
    private String destinoDirecionamento;

    @Column(name = "motivo_direcionamento", nullable = false, length = 255)
    private String motivoDirecionamento;

    @Column(name = "direcionado_em", nullable = false)
    private OffsetDateTime direcionadoEm;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "amarrado_carga_lote",
            joinColumns = @JoinColumn(name = "amarrado_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "lote_id", nullable = false),
            uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_amarrado_carga_lote",
                        columnNames = {"amarrado_id", "lote_id"}),
                @UniqueConstraint(name = "uk_lote_amarrado_carga", columnNames = "lote_id")
            })
    @OrderBy("codigo ASC")
    private List<LoteCarga> lotes = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        if (direcionadoEm == null) {
            direcionadoEm = agora;
        }
        normalizarCampos();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
        normalizarCampos();
    }

    public void adicionarLote(LoteCarga lote) {
        Objects.requireNonNull(lote, "O cargo lot deve ser informado.");
        boolean duplicado = lotes.stream().anyMatch(existente ->
                existente == lote || Objects.equals(existente.getId(), lote.getId()));
        if (duplicado) {
            throw new IllegalArgumentException("O cargo lot já pertence a este amarrado.");
        }
        lotes.add(lote);
    }

    public void registrarDirecionamento(String destino, String motivo) {
        destinoDirecionamento = textoObrigatorio(destino);
        motivoDirecionamento = textoObrigatorio(motivo);
        direcionadoEm = OffsetDateTime.now();
    }

    public boolean isMisto() {
        return getGruposArmazenagem().size() > 1;
    }

    public List<String> getGruposArmazenagem() {
        return lotes.stream()
                .map(LoteCarga::getItem)
                .filter(Objects::nonNull)
                .map(ItemConhecimentoCarga::getCodigoArmazenagem)
                .filter(valor -> valor != null && !valor.isBlank())
                .map(valor -> valor.trim().toUpperCase())
                .distinct()
                .sorted()
                .collect(Collectors.toUnmodifiableList());
    }

    private void normalizarCampos() {
        codigo = normalizar(codigo);
        visitaNavioId = textoObrigatorio(visitaNavioId);
        destinoDirecionamento = normalizar(destinoDirecionamento);
        motivoDirecionamento = textoObrigatorio(motivoDirecionamento);
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim().toUpperCase();
    }

    private String textoObrigatorio(String valor) {
        return valor == null ? null : valor.trim();
    }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(String visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public boolean isIntegro() { return integro; }
    public void setIntegro(boolean integro) { this.integro = integro; }
    public String getDestinoDirecionamento() { return destinoDirecionamento; }
    public String getMotivoDirecionamento() { return motivoDirecionamento; }
    public OffsetDateTime getDirecionadoEm() { return direcionadoEm; }
    public List<LoteCarga> getLotes() { return Collections.unmodifiableList(lotes); }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
}
