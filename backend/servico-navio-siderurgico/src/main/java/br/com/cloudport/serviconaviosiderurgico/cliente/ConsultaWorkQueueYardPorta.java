package br.com.cloudport.serviconaviosiderurgico.cliente;

import java.util.List;

public interface ConsultaWorkQueueYardPorta {

    List<WorkQueueValidacaoYardDto> listarParaValidacaoPlano(Long visitaNavioId);
}
