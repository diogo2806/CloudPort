package br.com.cloudport.servicoyard.configuracao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cloudport.servicoyard.container.servico.MovimentacaoTremListener;
import br.com.cloudport.servicoyard.edi.mensagem.EdiMensagemListenerServico;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

class ConsumidoresMigracaoConfiguracaoTest {

    private static final String CONTROLE_CONSUMIDORES = "${cloudport.runtime.consumers-enabled:true}";

    @Test
    void todosOsConsumidoresDoYardDevemPermitirDesativacaoDuranteAMigracao() throws Exception {
        assertAutoStartupConfiguravel(
                EdiMensagemListenerServico.class.getDeclaredMethod(
                        "receberCoprar",
                        br.com.cloudport.servicoyard.edi.dto.CoprarMensagemDto.class
                )
        );
        assertAutoStartupConfiguravel(
                EdiMensagemListenerServico.class.getDeclaredMethod(
                        "receberCoarri",
                        br.com.cloudport.servicoyard.edi.dto.CoarriMensagemDto.class
                )
        );
        assertAutoStartupConfiguravel(
                MovimentacaoTremListener.class.getDeclaredMethod(
                        "aoReceberMovimentacao",
                        br.com.cloudport.servicoyard.container.dto.MovimentacaoTremConcluidaEventoDto.class
                )
        );
    }

    private void assertAutoStartupConfiguravel(Method metodo) {
        RabbitListener listener = metodo.getAnnotation(RabbitListener.class);
        assertThat(listener).isNotNull();
        assertThat(listener.autoStartup()).isEqualTo(CONTROLE_CONSUMIDORES);
    }
}
