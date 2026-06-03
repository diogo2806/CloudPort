package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.util.ArrayList;
import java.util.List;

public class SequenciaEmbarqueDto {

    private List<PortoSequenciaDto> sequenciaPortos = new ArrayList<>();
    private int totalItensRestow;
    private boolean lifoValido;
    private List<ViolacaoEstivaDto> violacoesLifo = new ArrayList<>();

    public static class PortoSequenciaDto {
        private String codigoPorto;
        private int sequencia;
        private int totalItensDescarga;
        private int totalItensRestow;
        private List<String> itensRestow = new ArrayList<>();

        public String getCodigoPorto() { return codigoPorto; }
        public void setCodigoPorto(String codigoPorto) { this.codigoPorto = codigoPorto; }
        public int getSequencia() { return sequencia; }
        public void setSequencia(int sequencia) { this.sequencia = sequencia; }
        public int getTotalItensDescarga() { return totalItensDescarga; }
        public void setTotalItensDescarga(int totalItensDescarga) { this.totalItensDescarga = totalItensDescarga; }
        public int getTotalItensRestow() { return totalItensRestow; }
        public void setTotalItensRestow(int totalItensRestow) { this.totalItensRestow = totalItensRestow; }
        public List<String> getItensRestow() { return itensRestow; }
        public void setItensRestow(List<String> itensRestow) { this.itensRestow = itensRestow; }
    }

    public List<PortoSequenciaDto> getSequenciaPortos() { return sequenciaPortos; }
    public void setSequenciaPortos(List<PortoSequenciaDto> sequenciaPortos) { this.sequenciaPortos = sequenciaPortos; }

    public int getTotalItensRestow() { return totalItensRestow; }
    public void setTotalItensRestow(int totalItensRestow) { this.totalItensRestow = totalItensRestow; }

    public boolean isLifoValido() { return lifoValido; }
    public void setLifoValido(boolean lifoValido) { this.lifoValido = lifoValido; }

    public List<ViolacaoEstivaDto> getViolacoesLifo() { return violacoesLifo; }
    public void setViolacoesLifo(List<ViolacaoEstivaDto> violacoesLifo) { this.violacoesLifo = violacoesLifo; }
}
