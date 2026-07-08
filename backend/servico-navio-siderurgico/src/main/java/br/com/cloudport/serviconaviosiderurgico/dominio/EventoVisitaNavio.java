package br.com.cloudport.serviconaviosiderurgico.dominio;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "evento_visita_navio")
public class EventoVisitaNavio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visita_navio_id", nullable = false)
    private VisitaNavio visitaNavio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_operacao_id")
    private ItemOperacaoNavio itemOperacao;

    @Column(name = "tipo_evento", nullable = false, length = 80)
    private String tipoEvento;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(nullable = false, length = 120)
    private String usuario = "sistema";

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "dados_antes", length = 2000)
    private String dadosAntes;

    @Column(name = "dados_depois", length = 2000)
    private String dadosDepois;

    @PrePersist
    void prePersist() {
        criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public VisitaNavio getVisitaNavio() { return visitaNavio; }
    public void setVisitaNavio(VisitaNavio visitaNavio) { this.visitaNavio = visitaNavio; }
    public ItemOperacaoNavio getItemOperacao() { return itemOperacao; }
    public void setItemOperacao(ItemOperacaoNavio itemOperacao) { this.itemOperacao = itemOperacao; }
    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public String getDadosAntes() { return dadosAntes; }
    public void setDadosAntes(String dadosAntes) { this.dadosAntes = dadosAntes; }
    public String getDadosDepois() { return dadosDepois; }
    public void setDadosDepois(String dadosDepois) { this.dadosDepois = dadosDepois; }
}
