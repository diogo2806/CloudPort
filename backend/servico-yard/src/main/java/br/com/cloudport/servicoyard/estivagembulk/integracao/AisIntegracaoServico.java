package br.com.cloudport.servicoyard.estivagembulk.integracao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Stub de integração com AIS/GPS para rastreamento de posição do navio
 * em tempo real. Em produção integrar com AIS provider (MarineTraffic, etc).
 */
@Service
public class AisIntegracaoServico {

    private static final Logger log = LoggerFactory.getLogger(AisIntegracaoServico.class);

    @Value("${cloudport.integracao.ais.url:#{null}}")
    private String aisUrl;

    @Value("${cloudport.integracao.ais.habilitado:false}")
    private boolean habilitado;

    public Map<String, Object> obterPosicaoAtual(String imoNavio) {
        if (!habilitado || aisUrl == null) {
            log.debug("Integração AIS desabilitada — stub para IMO {}", imoNavio);
            return Map.of(
                    "imo", imoNavio,
                    "status", "STUB",
                    "mensagem", "Integração AIS não configurada",
                    "latitude", 0.0,
                    "longitude", 0.0
            );
        }
        log.info("Consultando posição AIS do navio IMO {}", imoNavio);
        return Map.of("imo", imoNavio, "status", "NAO_IMPLEMENTADO");
    }

    public Map<String, Object> estimarEta(String imoNavio, String codigoPortoDestino) {
        if (!habilitado) {
            log.debug("Integração AIS desabilitada — ETA não calculado para {}", codigoPortoDestino);
            return Map.of(
                    "imo", imoNavio,
                    "portoDestino", codigoPortoDestino,
                    "status", "STUB",
                    "etaEstimada", "N/A"
            );
        }
        log.info("Estimando ETA do navio {} para porto {}", imoNavio, codigoPortoDestino);
        return Map.of("status", "NAO_IMPLEMENTADO");
    }

    public void processarWebhookPosicao(Map<String, Object> payload) {
        log.info("Webhook AIS recebido: IMO={}, lat={}, lon={}",
                payload.get("imo"), payload.get("latitude"), payload.get("longitude"));
    }
}
