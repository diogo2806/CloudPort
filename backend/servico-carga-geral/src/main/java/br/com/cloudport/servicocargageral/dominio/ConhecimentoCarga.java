package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusConhecimentoCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoOperacaoConhecimento;
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

@Entity
@Table(name = "conhecimento_carga")
public class ConhecimentoCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false, length = 30)
    private TipoOperacaoConhecimento tipoOperacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusConhecimentoCarga status = StatusConhecimentoCarga.RASCUNHO;

    @Column(nullable = false, length = 180)
    private String embarcador;

    @Column(nullable = false, length = 180)
    private String consignatario;

    @Column(name = "cliente_id", length = 80)
    private String clienteId;

    @Column(name = "operador_id", length = 80)
    private String operadorId;

    @Column(name = "visita_navio_id", length = 80)
    private String visitaNavioId;

    @Column(name = "visita_veiculo_id", length = 80)
    private String visitaVeiculoId;

    @Column(name = "armazem_id", length = 80)
    private String armazemId;

    @Column(name = "porto_origem", length = 120)
    private String portoOrigem;

    @Column(name = "porto_destino", length = 120)
    private String portoDestino;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    @OneToMany(mappedBy = "conhecimento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequencia ASC")
    private List<ItemConhecimentoCarga> itens = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        numero = normalizar(numero);
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
        numero = normalizar(numero);
    }

    public void adicionarItem(ItemConhecimentoCarga item) {
        item.setConhecimento(this);
        itens.add(item);
        if (status == StatusConhecimentoCarga.RASCUNHO) {
            status = StatusConhecimentoCarga.MANIFESTADO;
        }
    }

    public void iniciarOperacao() {
        if (status != StatusConhecimentoCarga.CANCELADO && status != StatusConhecimentoCarga.CONCLUIDO) {
            status = StatusConhecimentoCarga.EM_OPERACAO;
        }
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim().toUpperCase();
    }

    public UUID getId() { return id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public TipoOperacaoConhecimento getTipoOperacao() { return tipoOperacao; }
    public void setTipoOperacao(TipoOperacaoConhecimento tipoOperacao) { this.tipoOperacao = tipoOperacao; }
    public StatusConhecimentoCarga getStatus() { return status; }
    public void setStatus(StatusConhecimentoCarga status) { this.status = status; }
    public String getEmbarcador() { return embarcador; }
    public void setEmbarcador(String embarcador) { this.embarcador = embarcador; }
    public String getConsignatario() { return consignatario; }
    public void setConsignatario(String consignatario) { this.consignatario = consignatario; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public String getOperadorId() { return operadorId; }
    public void setOperadorId(String operadorId) { this.operadorId = operadorId; }
    public String getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(String visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public String getVisitaVeiculoId() { return visitaVeiculoId; }
    public void setVisitaVeiculoId(String visitaVeiculoId) { this.visitaVeiculoId = visitaVeiculoId; }
    public String getArmazemId() { return armazemId; }
    public void setArmazemId(String armazemId) { this.armazemId = armazemId; }
    public String getPortoOrigem() { return portoOrigem; }
    public void setPortoOrigem(String portoOrigem) { this.portoOrigem = portoOrigem; }
    public String getPortoDestino() { return portoDestino; }
    public void setPortoDestino(String portoDestino) { this.portoDestino = portoDestino; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public List<ItemConhecimentoCarga> getItens() { return Collections.unmodifiableList(itens); }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
}
