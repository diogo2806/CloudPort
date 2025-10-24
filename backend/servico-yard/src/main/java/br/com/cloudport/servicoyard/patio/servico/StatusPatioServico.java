package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.StatusPatioDto;
import br.com.cloudport.servicoyard.patio.enumeracao.StatusServicoPatioEnum;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.MovimentoPatioRepositorio;
import java.time.LocalDateTime;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatusPatioServico {

    private final ConteinerPatioRepositorio conteinerPatioRepositorio;
    private final MovimentoPatioRepositorio movimentoPatioRepositorio;
    private final RabbitTemplate rabbitTemplate;

    public StatusPatioServico(ConteinerPatioRepositorio conteinerPatioRepositorio,
                              MovimentoPatioRepositorio movimentoPatioRepositorio,
                              RabbitTemplate rabbitTemplate) {
        this.conteinerPatioRepositorio = conteinerPatioRepositorio;
        this.movimentoPatioRepositorio = movimentoPatioRepositorio;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional(readOnly = true)
    public StatusPatioDto verificarDisponibilidade() {
        LocalDateTime verificadoEm = LocalDateTime.now();
        try {
            conteinerPatioRepositorio.count();
            movimentoPatioRepositorio.count();
            rabbitTemplate.execute(channel -> Boolean.TRUE);
            return new StatusPatioDto(
                    StatusServicoPatioEnum.DISPONIVEL,
                    "Serviço de pátio operacional.",
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
