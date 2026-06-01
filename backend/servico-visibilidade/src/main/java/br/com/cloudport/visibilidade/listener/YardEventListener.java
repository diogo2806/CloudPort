package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.entity.ConteinerLocalizacao;
import br.com.cloudport.visibilidade.repository.ConteinerLocalizacaoRepository;
import br.com.cloudport.visibilidade.service.CapacidadeYardService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class YardEventListener {

    @Autowired
    private ConteinerLocalizacaoRepository localizacaoRepository;

    @Autowired
    private CapacidadeYardService capacidadeYardService;

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_YARD_QUEUE)
    public void handleYardEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        String containerId = (String) event.get("containerId");
        String zona = (String) event.get("zona");
        String posicao = (String) event.get("posicao");

        if (containerId == null) return;

        if ("yard.container.stored".equals(eventType)) {
            ConteinerLocalizacao loc = localizacaoRepository.findByContainerId(containerId)
                    .orElseGet(() -> {
                        ConteinerLocalizacao nova = new ConteinerLocalizacao();
                        nova.setContainerId(containerId);
                        return nova;
                    });

            loc.setStatusAtual("no_yard");
            loc.setZona(zona);
            loc.setPosicao(posicao);
            loc.setDataAtualizacao(LocalDateTime.now());
            localizacaoRepository.save(loc);

            System.out.println("[Yard] Container " + containerId + " armazenado em " + zona + "-" + posicao);
        }

        if ("yard.capacity_updated".equals(eventType) && zona != null) {
            Integer ocupacao = (Integer) event.get("ocupacaoAtual");
            if (ocupacao != null) {
                capacidadeYardService.atualizarOcupacao(zona, ocupacao);
            }
        }
    }
}