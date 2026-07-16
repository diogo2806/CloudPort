package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class AplicacaoPlanoOtimizadoPatioDto {

    @NotBlank
    private String planoId;

    @NotNull
    private Long visitaNavioId;

    @NotBlank
    private String usuario;

    @Valid
    @NotEmpty
    private List<ItemPlanoOtimizadoDto> itens = new ArrayList<>();

    public String getPlanoId() {
        return planoId;
    }

    public void setPlanoId(String planoId) {
        this.planoId = planoId;
    }

    public Long getVisitaNavioId() {
        return visitaNavioId;
    }

    public void setVisitaNavioId(Long visitaNavioId) {
        this.visitaNavioId = visitaNavioId;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public List<ItemPlanoOtimizadoDto> getItens() {
        return itens;
    }

    public void setItens(List<ItemPlanoOtimizadoDto> itens) {
        this.itens = itens;
    }

    public static class ItemPlanoOtimizadoDto {

        @NotNull
        private Long ordemTrabalhoPatioId;

        @NotNull
        private Long itemOperacaoNavioId;

        @NotBlank
        private String codigoConteiner;

        @NotNull
        private Integer linha;

        @NotNull
        private Integer coluna;

        @NotBlank
        private String camada;

        @NotBlank
        private String equipamento;

        @NotNull
        @Min(0)
        private Integer prioridadeOperacional;

        public Long getOrdemTrabalhoPatioId() {
            return ordemTrabalhoPatioId;
        }

        public void setOrdemTrabalhoPatioId(Long ordemTrabalhoPatioId) {
            this.ordemTrabalhoPatioId = ordemTrabalhoPatioId;
        }

        public Long getItemOperacaoNavioId() {
            return itemOperacaoNavioId;
        }

        public void setItemOperacaoNavioId(Long itemOperacaoNavioId) {
            this.itemOperacaoNavioId = itemOperacaoNavioId;
        }

        public String getCodigoConteiner() {
            return codigoConteiner;
        }

        public void setCodigoConteiner(String codigoConteiner) {
            this.codigoConteiner = codigoConteiner;
        }

        public Integer getLinha() {
            return linha;
        }

        public void setLinha(Integer linha) {
            this.linha = linha;
        }

        public Integer getColuna() {
            return coluna;
        }

        public void setColuna(Integer coluna) {
            this.coluna = coluna;
        }

        public String getCamada() {
            return camada;
        }

        public void setCamada(String camada) {
            this.camada = camada;
        }

        public String getEquipamento() {
            return equipamento;
        }

        public void setEquipamento(String equipamento) {
            this.equipamento = equipamento;
        }

        public Integer getPrioridadeOperacional() {
            return prioridadeOperacional;
        }

        public void setPrioridadeOperacional(Integer prioridadeOperacional) {
            this.prioridadeOperacional = prioridadeOperacional;
        }
    }
}
