package br.com.cloudport.serviconavio.atracacao.entidade;

import br.com.cloudport.serviconavio.linha.entidade.ServicoLinha;
import br.com.cloudport.serviconavio.navio.entidade.Navio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "visita_navio")
public class VisitaNavio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Long identificador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "navio_id")
    private Navio navio;

    @Column(name = "numero_viagem", nullable = false, length = 40)
    private String numeroViagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "berco_id")
    private Berco berco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_linha_id")
    private ServicoLinha servico;

    @Column(name = "chegada_prevista")
    private LocalDateTime chegadaPrevista;

    @Column(name = "chegada_efetiva")
    private LocalDateTime chegadaEfetiva;

    @Column(name = "atracacao_prevista", nullable = false)
    private LocalDateTime atracacaoPrevista;

    @Column(name = "atracacao_efetiva")
    private LocalDateTime atracacaoEfetiva;

    @Column(name = "desatracacao_prevista", nullable = false)
    private LocalDateTime desatracacaoPrevista;

    @Column(name = "desatracacao_efetiva")
    private LocalDateTime desatracacaoEfetiva;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusVisitaNavio status;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @OneToMany(mappedBy = "visita", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OperacaoNavioConteiner> operacoes = new ArrayList<>();

    public void adicionarOperacao(OperacaoNavioConteiner operacao) {
        operacao.setVisita(this);
        this.operacoes.add(operacao);
    }

    public void removerOperacao(OperacaoNavioConteiner operacao) {
        this.operacoes.remove(operacao);
        operacao.setVisita(null);
    }

    public Long getIdentificador() {
        return identificador;
    }

    public void setIdentificador(Long identificador) {
        this.identificador = identificador;
    }

    public Navio getNavio() {
        return navio;
    }

    public void setNavio(Navio navio) {
        this.navio = navio;
    }

    public String getNumeroViagem() {
        return numeroViagem;
    }

    public void setNumeroViagem(String numeroViagem) {
        this.numeroViagem = numeroViagem;
    }

    public Berco getBerco() {
        return berco;
    }

    public void setBerco(Berco berco) {
        this.berco = berco;
    }

    public ServicoLinha getServico() {
        return servico;
    }

    public void setServico(ServicoLinha servico) {
        this.servico = servico;
    }

    public LocalDateTime getChegadaPrevista() {
        return chegadaPrevista;
    }

    public void setChegadaPrevista(LocalDateTime chegadaPrevista) {
        this.chegadaPrevista = chegadaPrevista;
    }

    public LocalDateTime getChegadaEfetiva() {
        return chegadaEfetiva;
    }

    public void setChegadaEfetiva(LocalDateTime chegadaEfetiva) {
        this.chegadaEfetiva = chegadaEfetiva;
    }

    public LocalDateTime getAtracacaoPrevista() {
        return atracacaoPrevista;
    }

    public void setAtracacaoPrevista(LocalDateTime atracacaoPrevista) {
        this.atracacaoPrevista = atracacaoPrevista;
    }

    public LocalDateTime getAtracacaoEfetiva() {
        return atracacaoEfetiva;
    }

    public void setAtracacaoEfetiva(LocalDateTime atracacaoEfetiva) {
        this.atracacaoEfetiva = atracacaoEfetiva;
    }

    public LocalDateTime getDesatracacaoPrevista() {
        return desatracacaoPrevista;
    }

    public void setDesatracacaoPrevista(LocalDateTime desatracacaoPrevista) {
        this.desatracacaoPrevista = desatracacaoPrevista;
    }

    public LocalDateTime getDesatracacaoEfetiva() {
        return desatracacaoEfetiva;
    }

    public void setDesatracacaoEfetiva(LocalDateTime desatracacaoEfetiva) {
        this.desatracacaoEfetiva = desatracacaoEfetiva;
    }

    public StatusVisitaNavio getStatus() {
        return status;
    }

    public void setStatus(StatusVisitaNavio status) {
        this.status = status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public List<OperacaoNavioConteiner> getOperacoes() {
        return operacoes;
    }

    public void setOperacoes(List<OperacaoNavioConteiner> operacoes) {
        this.operacoes = operacoes;
    }
}
