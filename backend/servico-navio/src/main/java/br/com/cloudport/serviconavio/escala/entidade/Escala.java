package br.com.cloudport.serviconavio.escala.entidade;

import br.com.cloudport.serviconavio.navio.entidade.Navio;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "escala")
public class Escala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "navio_id", nullable = false)
    private Navio navio;

    @Column(name = "viagem_entrada", nullable = false, length = 20)
    private String viagemEntrada;

    @Column(name = "viagem_saida", length = 20)
    private String viagemSaida;

    @Enumerated(EnumType.STRING)
    @Column(name = "fase", nullable = false, length = 20)
    private FaseEscala fase;

    @Column(name = "chegada_prevista", nullable = false)
    private LocalDateTime chegadaPrevista;

    @Column(name = "atracacao_prevista")
    private LocalDateTime atracacaoPrevista;

    @Column(name = "partida_prevista")
    private LocalDateTime partidaPrevista;

    @Column(name = "chegada_efetiva")
    private LocalDateTime chegadaEfetiva;

    @Column(name = "atracacao_efetiva")
    private LocalDateTime atracacaoEfetiva;

    @Column(name = "partida_efetiva")
    private LocalDateTime partidaEfetiva;

    @Column(name = "berco_previsto", length = 20)
    private String bercoPrevisto;

    @Column(name = "berco_atual", length = 20)
    private String bercoAtual;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @ElementCollection
    @CollectionTable(name = "escala_descarga",
            joinColumns = @JoinColumn(name = "escala_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_escala_descarga_conteiner",
                    columnNames = {"escala_id", "codigo_conteiner"}))
    @OrderColumn(name = "ordem_descarga")
    private List<OperacaoConteinerEscala> listaDescarga = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "escala_carga",
            joinColumns = @JoinColumn(name = "escala_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_escala_carga_conteiner",
                    columnNames = {"escala_id", "codigo_conteiner"}))
    @OrderColumn(name = "ordem_carga")
    private List<OperacaoConteinerEscala> listaCarga = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Navio getNavio() {
        return navio;
    }

    public void setNavio(Navio navio) {
        this.navio = navio;
    }

    public String getViagemEntrada() {
        return viagemEntrada;
    }

    public void setViagemEntrada(String viagemEntrada) {
        this.viagemEntrada = viagemEntrada;
    }

    public String getViagemSaida() {
        return viagemSaida;
    }

    public void setViagemSaida(String viagemSaida) {
        this.viagemSaida = viagemSaida;
    }

    public FaseEscala getFase() {
        return fase;
    }

    public void setFase(FaseEscala fase) {
        this.fase = fase;
    }

    public LocalDateTime getChegadaPrevista() {
        return chegadaPrevista;
    }

    public void setChegadaPrevista(LocalDateTime chegadaPrevista) {
        this.chegadaPrevista = chegadaPrevista;
    }

    public LocalDateTime getAtracacaoPrevista() {
        return atracacaoPrevista;
    }

    public void setAtracacaoPrevista(LocalDateTime atracacaoPrevista) {
        this.atracacaoPrevista = atracacaoPrevista;
    }

    public LocalDateTime getPartidaPrevista() {
        return partidaPrevista;
    }

    public void setPartidaPrevista(LocalDateTime partidaPrevista) {
        this.partidaPrevista = partidaPrevista;
    }

    public LocalDateTime getChegadaEfetiva() {
        return chegadaEfetiva;
    }

    public void setChegadaEfetiva(LocalDateTime chegadaEfetiva) {
        this.chegadaEfetiva = chegadaEfetiva;
    }

    public LocalDateTime getAtracacaoEfetiva() {
        return atracacaoEfetiva;
    }

    public void setAtracacaoEfetiva(LocalDateTime atracacaoEfetiva) {
        this.atracacaoEfetiva = atracacaoEfetiva;
    }

    public LocalDateTime getPartidaEfetiva() {
        return partidaEfetiva;
    }

    public void setPartidaEfetiva(LocalDateTime partidaEfetiva) {
        this.partidaEfetiva = partidaEfetiva;
    }

    public String getBercoPrevisto() {
        return bercoPrevisto;
    }

    public void setBercoPrevisto(String bercoPrevisto) {
        this.bercoPrevisto = bercoPrevisto;
    }

    public String getBercoAtual() {
        return bercoAtual;
    }

    public void setBercoAtual(String bercoAtual) {
        this.bercoAtual = bercoAtual;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public List<OperacaoConteinerEscala> getListaDescarga() {
        return listaDescarga;
    }

    public void definirListaDescarga(List<OperacaoConteinerEscala> listaDescarga) {
        this.listaDescarga.clear();
        if (listaDescarga != null) {
            this.listaDescarga.addAll(listaDescarga);
        }
    }

    public List<OperacaoConteinerEscala> getListaCarga() {
        return listaCarga;
    }

    public void definirListaCarga(List<OperacaoConteinerEscala> listaCarga) {
        this.listaCarga.clear();
        if (listaCarga != null) {
            this.listaCarga.addAll(listaCarga);
        }
    }

    @PrePersist
    public void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    public void aoAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
