package br.com.cloudport.serviconaviosiderurgico.cliente;

import java.util.List;

/** Porta de consulta ao mapa de posições do módulo Yard. */
public interface PosicaoPatioYardCliente {

    List<PosicaoPatioYardDTO> listarPosicoes();

    class PosicaoPatioYardDTO {
        private Long id;
        private Integer linha;
        private Integer coluna;
        private String camadaOperacional;
        private boolean ocupada;
        private String codigoConteiner;
        private String statusConteiner;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Integer getLinha() { return linha; }
        public void setLinha(Integer linha) { this.linha = linha; }
        public Integer getColuna() { return coluna; }
        public void setColuna(Integer coluna) { this.coluna = coluna; }
        public String getCamadaOperacional() { return camadaOperacional; }
        public void setCamadaOperacional(String camadaOperacional) { this.camadaOperacional = camadaOperacional; }
        public boolean isOcupada() { return ocupada; }
        public void setOcupada(boolean ocupada) { this.ocupada = ocupada; }
        public String getCodigoConteiner() { return codigoConteiner; }
        public void setCodigoConteiner(String codigoConteiner) { this.codigoConteiner = codigoConteiner; }
        public String getStatusConteiner() { return statusConteiner; }
        public void setStatusConteiner(String statusConteiner) { this.statusConteiner = statusConteiner; }

        public String identificador() {
            return id == null ? null : String.valueOf(id);
        }
    }
}
