package br.com.cloudport.serviconavio.navio.entidade;

import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "navio")
public class Navio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Long identificador;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "codigo_imo", nullable = false, unique = true, length = 10)
    private String codigoImo;

    @Column(name = "pais_bandeira", nullable = false, length = 60)
    private String paisBandeira;

    @Column(name = "empresa_armadora", nullable = false, length = 80)
    private String empresaArmadora;

    @Column(name = "capacidade_teu", nullable = false)
    private Integer capacidadeTeu;

    @Column(name = "loa_metros", precision = 6, scale = 2)
    private BigDecimal loaMetros;

    @Column(name = "calado_maximo_metros", precision = 5, scale = 2)
    private BigDecimal caladoMaximoMetros;

    @Column(name = "call_sign", length = 15)
    private String callSign;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    public Long getIdentificador() { return identificador; }
    public void setIdentificador(Long identificador) { this.identificador = identificador; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCodigoImo() { return codigoImo; }
    public void setCodigoImo(String codigoImo) { this.codigoImo = codigoImo; }
    public String getPaisBandeira() { return paisBandeira; }
    public void setPaisBandeira(String paisBandeira) { this.paisBandeira = paisBandeira; }
    public String getEmpresaArmadora() { return empresaArmadora; }
    public void setEmpresaArmadora(String empresaArmadora) { this.empresaArmadora = empresaArmadora; }
    public Integer getCapacidadeTeu() { return capacidadeTeu; }
    public void setCapacidadeTeu(Integer capacidadeTeu) { this.capacidadeTeu = capacidadeTeu; }
    public BigDecimal getLoaMetros() { return loaMetros; }
    public void setLoaMetros(BigDecimal loaMetros) { this.loaMetros = loaMetros; }
    public BigDecimal getCaladoMaximoMetros() { return caladoMaximoMetros; }
    public void setCaladoMaximoMetros(BigDecimal caladoMaximoMetros) { this.caladoMaximoMetros = caladoMaximoMetros; }
    public String getCallSign() { return callSign; }
    public void setCallSign(String callSign) { this.callSign = callSign; }
    public Long getVersao() { return versao; }
    public void setVersao(Long versao) { this.versao = versao; }
}
