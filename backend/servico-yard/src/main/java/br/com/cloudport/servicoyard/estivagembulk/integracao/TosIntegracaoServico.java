package br.com.cloudport.servicoyard.estivagembulk.integracao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Stub de integração com TOS (Terminal Operating System) para agendamento
 * de berço e confirmação de janela operacional.
 */
@Service
public class TosIntegracaoServico {

    private static final Logger log = LoggerFactory.getLogger(TosIntegracaoServico.class);

    @Value("${cloudport.integracao.tos.url:#{null}}")
    private String tosUrl;

    @Value("${cloudport.integracao.tos.habilitado:false}")
    private boolean habilitado;

    public Map<String, Object> consultarDisponibilidadeBerco(String codigoPorto, String dataEta) {
        if (!habilitado || tosUrl == null) {
            log.debug("Integração TOS desabilitada — stub para berço em {}", codigoPorto);
            return Map.of(
                    "codigoPorto", codigoPorto,
                    "disponivel", true,
                    "status", "STUB",
                    "mensagem", "Integração TOS não configurada"
            );
        }
        log.info("Consultando disponibilidade de berço em {} para ETA {}", codigoPorto, dataEta);
        return Map.of("disponivel", false, "status", "NAO_IMPLEMENTADO");
    }

    public boolean reservarBerco(String codigoViagem, String codigoPorto, String dataEta) {
        if (!habilitado) {
            log.debug("Integração TOS desabilitada — reserva de berço não realizada");
            return false;
        }
        log.info("Reservando berço para viagem {} no porto {} — ETA {}", codigoViagem, codigoPorto, dataEta);
        return true;
    }

    public Map<String, Object> obterStatusOperacao(String codigoViagem) {
        if (!habilitado) {
            return Map.of("codigoViagem", codigoViagem, "status", "STUB");
        }
        log.info("Consultando status operacional da viagem {} no TOS", codigoViagem);
        return Map.of("codigoViagem", codigoViagem, "status", "NAO_IMPLEMENTADO");
    }
}
