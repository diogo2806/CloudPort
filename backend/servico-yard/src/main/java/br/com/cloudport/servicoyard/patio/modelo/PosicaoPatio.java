package br.com.cloudport.servicoyard.patio.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "posicao_patio", uniqueConstraints = {
        @UniqueConstraint(name = "uk_posicao_unica", columnNames = {"linha", "coluna", "camada_operacional"})
})
public class PosicaoPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "linha", nullable = false)
    private Integer linha;

    @Column(name = "coluna", nullable = false)
    private Integer coluna;

    @Column(name = "camada_operacional", nullable = false, length = 40)
    private String camadaOperacional;

    @Column(name = "bloco", length = 40)
    private String bloco;

    @Column(name = "bloqueada", nullable = false)
    private boolean bloqueada;

    @Column(name = "interditada", nullable = false)
    private boolean interditada;

    @Column(name = "area_permitida", nullable = false)
    private boolean areaPermitida = true;

    @Column(name = "tipos_carga_permitidos", length = 500)
    private String tiposCargaPermitidos;

    @Column(name = "peso_maximo_toneladas", precision = 12, scale = 3)
    private BigDecimal pesoMaximoToneladas;

    @Column(name = "altura_maxima_metros", precision = 8, scale = 3)
    private BigDecimal alturaMaximaMetros;

    @Column(name = "camada_maxima")
    private Integer camadaMaxima;

    @Column(name = "capacidade_pilha")
    private Integer capacidadePilha;

    @Column(name = "reserva_chave", length = 120)
    private String reservaChave;

    @Column(name = "reserva_codigo_conteiner", length = 30)
    private String reservaCodigoConteiner;

    @Column(name = "reserva_expira_em")
    private LocalDateTime reservaExpiraEm;

    public PosicaoPatio() {
    }

    public PosicaoPatio(Long id, Integer linha, Integer coluna, String camadaOperacional) {
        this.id = id;
        this.linha = linha;
        this.coluna = coluna;
        this.camadaOperacional = camadaOperacional;
    }

    public boolean possuiReservaAtiva(LocalDateTime referencia) {
        return reservaChave != null
                && reservaExpiraEm != null
                && reservaExpiraEm.isAfter(referencia);
    }

    public void reservar(String chave, String codigoConteiner, LocalDateTime expiraEm) {
        this.reservaChave = chave;
        this.reservaCodigoConteiner = codigoConteiner;
        this.reservaExpiraEm = expiraEm;
    }

    public void liberarReserva() {
        this.reservaChave = null;
        this.reservaCodigoConteiner = null;
        this.reservaExpiraEm = null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getLinha() {
        return linha;
    }

    public void setLinha(Integer linha) {
        this.linha = linha;
    }

    public Integer getColuna() {
        return coluna;
    }

    public void setColuna(Integer coluna) {
        this.coluna = coluna;
    }

    public String getCamadaOperacional() {
        return camadaOperacional;
    }

    public void setCamadaOperacional(String camadaOperacional) {
        this.camadaOperacional = camadaOperacional;
    }

    public String getBloco() {
        return bloco;
    }

    public void setBloco(String bloco) {
        this.bloco = bloco;
    }

    public boolean isBloqueada() {
        return bloqueada;
    }

    public void setBloqueada(boolean bloqueada) {
        this.bloqueada = bloqueada;
    }

    public boolean isInterditada() {
        return interditada;
    }

    public void setInterditada(boolean interditada) {
        this.interditada = interditada;
    }

    public boolean isAreaPermitida() {
        return areaPermitida;
    }

    public void setAreaPermitida(boolean areaPermitida) {
        this.areaPermitida = areaPermitida;
    }

    public String getTiposCargaPermitidos() {
        return tiposCargaPermitidos;
    }

    public void setTiposCargaPermitidos(String tiposCargaPermitidos) {
        this.tiposCargaPermitidos = tiposCargaPermitidos;
    }

    public BigDecimal getPesoMaximoToneladas() {
        return pesoMaximoToneladas;
    }

    public void setPesoMaximoToneladas(BigDecimal pesoMaximoToneladas) {
        this.pesoMaximoToneladas = pesoMaximoToneladas;
    }

    public BigDecimal getAlturaMaximaMetros() {
        return alturaMaximaMetros;
    }

    public void setAlturaMaximaMetros(BigDecimal alturaMaximaMetros) {
        this.alturaMaximaMetros = alturaMaximaMetros;
    }

    public Integer getCamadaMaxima() {
        return camadaMaxima;
    }

    public void setCamadaMaxima(Integer camadaMaxima) {
        this.camadaMaxima = camadaMaxima;
    }

    public Integer getCapacidadePilha() {
        return capacidadePilha;
    }

    public void setCapacidadePilha(Integer capacidadePilha) {
        this.capacidadePilha = capacidadePilha;
    }

    public String getReservaChave() {
        return reservaChave;
    }

    public void setReservaChave(String reservaChave) {
        this.reservaChave = reservaChave;
    }

    public String getReservaCodigoConteiner() {
        return reservaCodigoConteiner;
    }

    public void setReservaCodigoConteiner(String reservaCodigoConteiner) {
        this.reservaCodigoConteiner = reservaCodigoConteiner;
    }

    public LocalDateTime getReservaExpiraEm() {
        return reservaExpiraEm;
    }

    public void setReservaExpiraEm(LocalDateTime reservaExpiraEm) {
        this.reservaExpiraEm = reservaExpiraEm;
    }
}
