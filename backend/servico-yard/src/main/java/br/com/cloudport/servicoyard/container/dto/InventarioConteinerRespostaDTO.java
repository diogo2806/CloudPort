package br.com.cloudport.servicoyard.container.dto;

import java.util.List;

public class InventarioConteinerRespostaDTO {

    private final InventarioConteinerResumoDTO resumo;
    private final List<ConteinerDetalheDTO> conteineres;

    public InventarioConteinerRespostaDTO(InventarioConteinerResumoDTO resumo,
                                          List<ConteinerDetalheDTO> conteineres) {
        this.resumo = resumo;
        this.conteineres = List.copyOf(conteineres);
    }

    public InventarioConteinerResumoDTO getResumo() {
        return resumo;
    }

    public List<ConteinerDetalheDTO> getConteineres() {
        return conteineres;
    }
}
