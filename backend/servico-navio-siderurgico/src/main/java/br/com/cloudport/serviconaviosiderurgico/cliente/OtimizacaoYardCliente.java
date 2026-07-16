package br.com.cloudport.serviconaviosiderurgico.cliente;

import java.util.Map;

/**
 * Porta de otimização do Yard usada pelo módulo Navio Siderúrgico.
 *
 * <p>O runtime modular fornece uma implementação local. O adaptador HTTP é
 * mantido somente para execução standalone e rollback.</p>
 */
public interface OtimizacaoYardCliente {

    Map<String, Object> otimizar(Map<String, Object> requisicao);
}
