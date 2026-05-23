package br.com.cloudport.serviconavio.estiva.entidade;

/**
 * Tipos de carga de contêiner relevantes para a estiva. Espelha os tipos
 * adotados no serviço de pátio (servico-yard) para manter a integração coerente.
 */
public enum TipoCargaConteiner {
    SECO,
    REFRIGERADO,
    PERIGOSO,
    GRANELEIRO,
    OUTRO
}
