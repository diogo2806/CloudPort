package br.com.cloudport.servicocargageral.dominio;

import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "identificacao_cargo_lot")
public class IdentificacaoCargoLot {

    public enum TipoIdentificacao {
        CODIGO_BARRAS,
        QR_CODE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "codigo", nullable = false, unique = true, length = 160)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoIdentificacao tipo;

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Column(name = "embalagem_referencia", length = 160)
    private String embalagemReferencia;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "registrado_por", nullable = false, length = 120)
    private String registradoPor;

    @Column(name = "registrado_em", nullable = false)
    private OffsetDateTime registradoEm;

    @PrePersist
    void prePersist() {
        codigo = normalizar(codigo);
        registradoEm = OffsetDateTime.now();
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim().toUpperCase();
    }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public TipoIdentificacao getTipo() { return tipo; }
    public void setTipo(TipoIdentificacao tipo) { this.tipo = tipo; }
    public UUID getLoteId() { return loteId; }
    public void setLoteId(UUID loteId) { this.loteId = loteId; }
    public String getEmbalagemReferencia() { return embalagemReferencia; }
    public void setEmbalagemReferencia(String embalagemReferencia) { this.embalagemReferencia = embalagemReferencia; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }
    public OffsetDateTime getRegistradoEm() { return registradoEm; }
}
