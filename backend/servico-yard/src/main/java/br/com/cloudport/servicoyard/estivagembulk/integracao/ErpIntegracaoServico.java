package br.com.cloudport.servicoyard.estivagembulk.integracao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Stub de integração com ERP (SAP) para importação de ordens de venda e
 * dados de produção MES. Em produção substituir pelos clientes SAP RFC/REST.
 */
@Service
public class ErpIntegracaoServico {

    private static final Logger log = LoggerFactory.getLogger(ErpIntegracaoServico.class);

    @Value("${cloudport.integracao.erp.url:#{null}}")
    private String erpUrl;

    @Value("${cloudport.integracao.erp.habilitado:false}")
    private boolean habilitado;

    public List<Map<String, Object>> buscarOrdemVenda(String numeroOrdem) {
        if (!habilitado || erpUrl == null) {
            log.debug("Integração ERP desabilitada — retornando stub para ordem {}", numeroOrdem);
            return List.of(Map.of(
                    "numeroOrdem", numeroOrdem,
                    "status", "STUB",
                    "mensagem", "Integração ERP não configurada"
            ));
        }
        log.info("Buscando ordem de venda {} no ERP: {}", numeroOrdem, erpUrl);
        return List.of();
    }

    public List<Map<String, Object>> buscarItensEmbarquesPorViagem(String codigoViagem) {
        if (!habilitado || erpUrl == null) {
            log.debug("Integração ERP desabilitada — stub para viagem {}", codigoViagem);
            return List.of();
        }
        log.info("Buscando itens de embarque para viagem {} no ERP", codigoViagem);
        return List.of();
    }

    public boolean confirmarEmbarque(String codigoViagem, List<String> codigosBobinas) {
        if (!habilitado) {
            log.debug("Integração ERP desabilitada — confirmação de embarque não enviada");
            return false;
        }
        log.info("Confirmando embarque de {} itens para viagem {} no ERP", codigosBobinas.size(), codigoViagem);
        return true;
    }
}
