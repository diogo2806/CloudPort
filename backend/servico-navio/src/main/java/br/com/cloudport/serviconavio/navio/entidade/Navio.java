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
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "identificador") private Long identificador;
    @Column(name = "nome", nullable = false, length = 120) private String nome;
    @Column(name = "codigo_imo", nullable = false, unique = true, length = 10) private String codigoImo;
    @Column(name = "pais_bandeira", nullable = false, length = 60) private String paisBandeira;
    @Column(name = "empresa_armadora", nullable = false, length = 80) private String empresaArmadora;
    @Column(name = "capacidade_teu", nullable = false) private Integer capacidadeTeu;
    @Column(name = "loa_metros", precision = 6, scale = 2) private BigDecimal loaMetros;
    @Column(name = "calado_maximo_metros", precision = 5, scale = 2) private BigDecimal caladoMaximoMetros;
    @Column(name = "call_sign", length = 15) private String callSign;
    @Version @Column(name = "versao", nullable = false) private Long versao;
    public Long getIdentificador() { return identificador; }
    public void setIdentificador(Long valor) { identificador = valor; }
    public String getNome() { return nome; }
    public void setNome(String valor) { nome = valor; }
    public String getCodigoImo() { return codigoImo; }
    public void setCodigoImo(String valor) { codigoImo = valor; }
    public String getPaisBandeira() { return paisBandeira; }
    public void setPaisBandeira(String valor) { paisBandeira = valor; }
    public String getEmpresaArmadora() { return empresaArmadora; }
    public void setEmpresaArmadora(String valor) { empresaArmadora = valor; }
    public Integer getCapacidadeTeu() { return capacidadeTeu; }
    public void setCapacidadeTeu(Integer valor) { capacidadeTeu = valor; }
    public BigDecimal getLoaMetros() { return loaMetros; }
    public void setLoaMetros(BigDecimal valor) { loaMetros = valor; }
    public BigDecimal getCaladoMaximoMetros() { return caladoMaximoMetros; }
    public void setCaladoMaximoMetros(BigDecimal valor) { caladoMaximoMetros = valor; }
    public String getCallSign() { return callSign; }
    public void setCallSign(String valor) { callSign = valor; }
    public Long getVersao() { return versao; }
    public void setVersao(Long valor) { versao = valor; }
}
