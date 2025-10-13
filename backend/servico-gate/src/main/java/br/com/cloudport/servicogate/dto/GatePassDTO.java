package br.com.cloudport.servicogate.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GatePassDTO {

    private Long id;
    private String codigo;
    private String status;
    private String statusDescricao;
    private LocalDateTime dataEntrada;
    private LocalDateTime dataSaida;
    private List<GateEventDTO> eventos;
    private String token;

    public GatePassDTO() {
    }

    public GatePassDTO(Long id, String codigo, String status, String statusDescricao,
                       LocalDateTime dataEntrada, LocalDateTime dataSaida, List<GateEventDTO> eventos,
                       String token) {
        this.id = id;
        this.codigo = codigo;
        this.status = status;
        this.statusDescricao = statusDescricao;
        this.dataEntrada = dataEntrada;
        this.dataSaida = dataSaida;
        this.eventos = eventos;
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusDescricao() {
        return statusDescricao;
    }

    public void setStatusDescricao(String statusDescricao) {
        this.statusDescricao = statusDescricao;
    }

    public LocalDateTime getDataEntrada() {
        return dataEntrada;
    }

    public void setDataEntrada(LocalDateTime dataEntrada) {
        this.dataEntrada = dataEntrada;
    }

    public LocalDateTime getDataSaida() {
        return dataSaida;
    }

    public void setDataSaida(LocalDateTime dataSaida) {
        this.dataSaida = dataSaida;
    }

    public List<GateEventDTO> getEventos() {
        return eventos;
    }

    public void setEventos(List<GateEventDTO> eventos) {
        this.eventos = eventos;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
