package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.entity.ConteinerLocalizacao;
import br.com.cloudport.visibilidade.repository.ConteinerLocalizacaoRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class GateEventListener {

    @Autowired
    private ConteinerLocalizacaoRepository localizacaoRepository;

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_GATE_QUEUE)
    public void handleGateEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        String containerId = (String) event.get("containerId");

        if (containerId == null) return;

        if ("gate.container.entered".equals(eventType)) {
            ConteinerLocalizacao loc = localizacaoRepository.findByContainerId(containerId)
                    .orElseGet(() -> {
                        ConteinerLocalizacao nova = new ConteinerLocalizacao();
                        nova.setContainerId(containerId);
                        return nova;
                    });

            loc.setStatusAtual("no_yard");
            loc.setDataAtualizacao(LocalDateTime.now());
            localizacaoRepository.save(loc);

            System.out.println("[Gate] Container " + containerId + " atualizado para status 'no_yard'");
        }

        if ("gate.container.exited".equals(eventType)) {
            localizacaoRepository.findByContainerId(containerId).ifPresent(loc -> {
                loc.setStatusAtual("saiu_do_porto");
                loc.setDataAtualizacao(LocalDateTime.now());
                localizacaoRepository.save(loc);
            });
            System.out.println("[Gate] Container " + containerId + " saiu do porto.");
        }
    }
}