package br.com.cloudport.servicoyard.scheduler.modelo;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(name = "plano_posicao_operacional", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_plano_posicao_assinatura_unidade",
                columnNames = {"assinatura_entrada", "codigo_container"})
})
public class PlanoPosicaoOperacional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_container", nullable = false, length = 30)
    private String codigoContainer;

    @Column(name = "ordem_trabalho_patio_id")
    private Long ordemTrabalhoPatioId;

    @Column(name = "bloco", length = 40)
    private String bloco;

    @Column(name = "linha", nullable = false)
    private Integer linha;

    @Column(name = "coluna", nullable = false)
    private Integer coluna;

    @Column(name = "camada", nullable = false, length = 40)
    private String camada;

    @Column(name = "equipamento_id", length = 80)
    private String equipamentoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPlanoPosicaoOperacional estado;

    @Column(name = "horizonte_inicio", nullable = false)
    private LocalDateTime horizonteInicio;

    @Column(name = "horizonte_fim", nullable = false)
    private LocalDateTime horizonteFim;

    @Column(name = "valido_ate", nullable = false)
    private LocalDateTime validoAte;

    @Column(name = "origem", nullable = false, length = 80)
    private String origem;

    @Column(name = "motivo", nullable = false, length = 1000)
    private String motivo;

    @Column(name = "assinatura_entrada", nullable = false, length = 128)
    private String assinaturaEntrada;

    @Column(name = "alterado_por", nullable = false, length = 120)
    private String alteradoPor;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @Column(name = "convertido_em")
    private LocalDateTime convertidoEm;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    public void prepararInclusao() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = criadoEm == null ? agora : criadoEm;
        atualizadoEm = agora;
        estado = estado == null ? EstadoPlanoPosicaoOperacional.TENTATIVO : estado;
    }

    @PreUpdate
    public void prepararAtualizacao() {
        atualizadoEm = LocalDateTime.now();
    }

    public boolean expiradoEm(LocalDateTime referencia) {
        return validoAte != null && !validoAte.isAfter(referencia);
    }

    public boolean correspondeAoDestino(Integer linhaDestino, Integer colunaDestino, String camadaDestino) {
        return java.util.Objects.equals(linha, linhaDestino)
                && java.util.Objects.equals(coluna, colunaDestino)
                && camada != null
                && camadaDestino != null
                && camada.equalsIgnoreCase(camadaDestino);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigoContainer() { return codigoContainer; }
    public void setCodigoContainer(String codigoContainer) { this.codigoContainer = codigoContainer; }
    public Long getOrdemTrabalhoPatioId() { return ordemTrabalhoPatioId; }
    public void setOrdemTrabalhoPatioId(Long ordemTrabalhoPatioId) { this.ordemTrabalhoPatioId = ordemTrabalhoPatioId; }
    public String getBloco() { return bloco; }
    public void setBloco(String bloco) { this.bloco = bloco; }
    public Integer getLinha() { return linha; }
    public void setLinha(Integer linha) { this.linha = linha; }
    public Integer getColuna() { return coluna; }
    public void setColuna(Integer coluna) { this.coluna = coluna; }
    public String getCamada() { return camada; }
    public void setCamada(String camada) { this.camada = camada; }
    public String getEquipamentoId() { return equipamentoId; }
    public void setEquipamentoId(String equipamentoId) { this.equipamentoId = equipamentoId; }
    public EstadoPlanoPosicaoOperacional getEstado() { return estado; }
    public void setEstado(EstadoPlanoPosicaoOperacional estado) { this.estado = estado; }
    public LocalDateTime getHorizonteInicio() { return horizonteInicio; }
    public void setHorizonteInicio(LocalDateTime horizonteInicio) { this.horizonteInicio = horizonteInicio; }
    public LocalDateTime getHorizonteFim() { return horizonteFim; }
    public void setHorizonteFim(LocalDateTime horizonteFim) { this.horizonteFim = horizonteFim; }
    public LocalDateTime getValidoAte() { return validoAte; }
    public void setValidoAte(LocalDateTime validoAte) { this.validoAte = validoAte; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getAssinaturaEntrada() { return assinaturaEntrada; }
    public void setAssinaturaEntrada(String assinaturaEntrada) { this.assinaturaEntrada = assinaturaEntrada; }
    public String getAlteradoPor() { return alteradoPor; }
    public void setAlteradoPor(String alteradoPor) { this.alteradoPor = alteradoPor; }
    public Long getVersao() { return versao; }
    public LocalDateTime getConvertidoEm() { return convertidoEm; }
    public void setConvertidoEm(LocalDateTime convertidoEm) { this.convertidoEm = convertidoEm; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
