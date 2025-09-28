package br.com.cloudport.servicogate.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class JanelaAtendimentoDTO {

    private Long id;
    private LocalDate data;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private Integer capacidade;
    private String canalEntrada;
    private String canalEntradaDescricao;

    public JanelaAtendimentoDTO() {
    }

    public JanelaAtendimentoDTO(Long id, LocalDate data, LocalTime horaInicio, LocalTime horaFim,
                                 Integer capacidade, String canalEntrada, String canalEntradaDescricao) {
        this.id = id;
        this.data = data;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.capacidade = capacidade;
        this.canalEntrada = canalEntrada;
        this.canalEntradaDescricao = canalEntradaDescricao;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }

    public void setHoraFim(LocalTime horaFim) {
        this.horaFim = horaFim;
    }

    public Integer getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(Integer capacidade) {
        this.capacidade = capacidade;
    }

    public String getCanalEntrada() {
        return canalEntrada;
    }

    public void setCanalEntrada(String canalEntrada) {
        this.canalEntrada = canalEntrada;
    }

    public String getCanalEntradaDescricao() {
        return canalEntradaDescricao;
    }

    public void setCanalEntradaDescricao(String canalEntradaDescricao) {
        this.canalEntradaDescricao = canalEntradaDescricao;
    }
}
