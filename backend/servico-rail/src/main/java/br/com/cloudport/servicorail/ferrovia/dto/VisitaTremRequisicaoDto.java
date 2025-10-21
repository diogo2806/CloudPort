package br.com.cloudport.servicorail.ferrovia.dto;

import br.com.cloudport.servicorail.comum.validacao.ValidacaoEntradaUtil;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.Valid;

public class VisitaTremRequisicaoDto {

    @NotBlank
    @Size(max = 40)
    private String identificadorTrem;

    @NotBlank
    @Size(max = 80)
    private String operadoraFerroviaria;

    @NotNull
    private LocalDateTime horaChegadaPrevista;

    @NotNull
    private LocalDateTime horaPartidaPrevista;

    @NotNull
    private StatusVisitaTrem statusVisita;

    @Valid
    private List<OperacaoConteinerVisitaRequisicaoDto> listaDescarga = new ArrayList<>();

    @Valid
    private List<OperacaoConteinerVisitaRequisicaoDto> listaCarga = new ArrayList<>();

    @Valid
    private List<VagaoVisitaRequisicaoDto> listaVagoes = new ArrayList<>();

    public String getIdentificadorTrem() {
        return identificadorTrem;
    }

    public void setIdentificadorTrem(String identificadorTrem) {
        this.identificadorTrem = ValidacaoEntradaUtil.limparTexto(identificadorTrem);
    }

    public String getOperadoraFerroviaria() {
        return operadoraFerroviaria;
    }

    public void setOperadoraFerroviaria(String operadoraFerroviaria) {
        this.operadoraFerroviaria = ValidacaoEntradaUtil.limparTexto(operadoraFerroviaria);
    }

    public LocalDateTime getHoraChegadaPrevista() {
        return horaChegadaPrevista;
    }

    public void setHoraChegadaPrevista(LocalDateTime horaChegadaPrevista) {
        this.horaChegadaPrevista = horaChegadaPrevista;
    }

    public LocalDateTime getHoraPartidaPrevista() {
        return horaPartidaPrevista;
    }

    public void setHoraPartidaPrevista(LocalDateTime horaPartidaPrevista) {
        this.horaPartidaPrevista = horaPartidaPrevista;
    }

    public StatusVisitaTrem getStatusVisita() {
        return statusVisita;
    }

    public void setStatusVisita(StatusVisitaTrem statusVisita) {
        this.statusVisita = statusVisita;
    }

    public List<OperacaoConteinerVisitaRequisicaoDto> getListaDescarga() {
        return listaDescarga;
    }

    public void setListaDescarga(List<OperacaoConteinerVisitaRequisicaoDto> listaDescarga) {
        this.listaDescarga = listaDescarga != null ? new ArrayList<>(listaDescarga) : new ArrayList<>();
    }

    public List<OperacaoConteinerVisitaRequisicaoDto> getListaCarga() {
        return listaCarga;
    }

    public void setListaCarga(List<OperacaoConteinerVisitaRequisicaoDto> listaCarga) {
        this.listaCarga = listaCarga != null ? new ArrayList<>(listaCarga) : new ArrayList<>();
    }

    public List<VagaoVisitaRequisicaoDto> getListaVagoes() {
        return listaVagoes;
    }

    public void setListaVagoes(List<VagaoVisitaRequisicaoDto> listaVagoes) {
        this.listaVagoes = listaVagoes != null ? new ArrayList<>(listaVagoes) : new ArrayList<>();
    }
}
