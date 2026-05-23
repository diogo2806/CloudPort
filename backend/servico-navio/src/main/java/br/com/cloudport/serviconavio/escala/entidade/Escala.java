package br.com.cloudport.serviconavio.escala.entidade;

import br.com.cloudport.serviconavio.navio.entidade.Navio;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

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
