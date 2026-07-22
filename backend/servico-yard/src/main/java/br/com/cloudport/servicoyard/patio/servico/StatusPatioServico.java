package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.StatusPatioDto;
import br.com.cloudport.servicoyard.patio.enumeracao.StatusServicoPatioEnum;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.MovimentoPatioRepositorio;
import java.time.LocalDateTime;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatusPatioServico {

    private final ConteinerPatioRepositorio conteinerPatioRepositorio;
    private final MovimentoPatioRepositorio movimentoPatioRepositorio;
    private final RabbitTemplate rabbitTemplate;
    private final boolean rabbitEnabled;

    @Autowired
    public StatusPatioServico(ConteinerPatioRepositorio conteinerPatioRepositorio,
                              MovimentoPatioRepositorio movimentoPatioRepositorio,
                              ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
                              @Value("${cloudport.messaging.rabbit.enabled:false}") boolean rabbitEnabled) {
        this(conteinerPatioRepositorio, movimentoPatioRepositorio,
                rabbitTemplateProvider.getIfAvailable(), rabbitEnabled);
    }

    public StatusPatioServico(ConteinerPatioRepositorio conteinerPatioRepositorio,
                              MovimentoPatioRepositorio movimentoPatioRepositorio,
                              RabbitTemplate rabbitTemplate,
                              boolean rabbitEnabled) {
        this.conteinerPatioRepositorio = conteinerPatioRepositorio;
        this.movimentoPatioRepositorio = movimentoPatioRepositorio;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitEnabled = rabbitEnabled;
    }

    @Transactional(readOnly = true)
    public StatusPatioDto verificarDisponibilidade() {
        LocalDateTime verificadoEm = LocalDateTime.now();
        try {
            conteinerPatioRepositorio.count();
            movimentoPatioRepositorio.count();
            if (rabbitEnabled) {
                if (rabbitTemplate == null) {
                    throw new IllegalStateException("RabbitMQ habilitado sem RabbitTemplate disponível");
                }
                rabbitTemplate.execute(channel -> Boolean.TRUE);
            }
            return new StatusPatioDto(
                    StatusServicoPatioEnum.DISPONIVEL,
                    rabbitEnabled
                            ? "Serviço de pátio operacional."
                            : "Serviço de pátio operacional com mensageria desabilitada.",
                    verificadoEm
            );
        } catch (Exception ex) {
            return new StatusPatioDto(
                    StatusServicoPatioEnum.INDISPONIVEL,
                    "Falha ao acessar dependências do serviço de pátio.",
                    verificadoEm
            );
        }
    }
}
