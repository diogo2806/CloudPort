package br.com.cloudport.servicoyard.container.servico;

import br.com.cloudport.servicoyard.container.dto.ConteinerDetalheDTO;
import br.com.cloudport.servicoyard.container.dto.HistoricoOperacaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroAlocacaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroLiberacaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroTransferenciaDTO;
import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.testes.BaseIntegracaoPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConteinerServicoTest extends BaseIntegracaoPostgresTest {

    @Autowired
    private ConteinerServico conteinerServico;

    @Test
    @Transactional
    void deveRegistrarAlocacaoESanitizarRestricoes() {
        RegistroAlocacaoDTO alocacao = new RegistroAlocacaoDTO();
        alocacao.setIdentificacao("CP-001");
        alocacao.setPosicaoPatio("P1-A");
        alocacao.setTipoCarga(TipoCargaConteiner.SECO);
        alocacao.setPesoToneladas(new BigDecimal("12.5"));
        alocacao.setRestricoes("<b>Perigoso</b>\nNenhum contato");

        ConteinerDetalheDTO detalhe = conteinerServico.registrarAlocacao(alocacao);

        assertThat(detalhe.getRestricoes()).isNotNull();
        assertThat(detalhe.getRestricoes()).doesNotContain("<");
        assertThat(detalhe.getRestricoes()).contains("Perigoso");

        List<HistoricoOperacaoDTO> historico = conteinerServico.consultarHistorico(detalhe.getIdentificador());
        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).getDescricao()).contains("Alocação registrada");
    }

    @Test
    @Transactional
    void naoPermiteTransferenciaAposLiberacao() {
        RegistroAlocacaoDTO alocacao = new RegistroAlocacaoDTO();
        alocacao.setIdentificacao("CP-002");
        alocacao.setPosicaoPatio("P1-B");
        alocacao.setTipoCarga(TipoCargaConteiner.REFRIGERADO);
        alocacao.setPesoToneladas(new BigDecimal("10.0"));
        ConteinerDetalheDTO conteiner = conteinerServico.registrarAlocacao(alocacao);

        RegistroLiberacaoDTO liberacao = new RegistroLiberacaoDTO();
        liberacao.setDestinoFinal("Navio Aurora");
        liberacao.setResponsavel("Fiscal Portuário");
        conteinerServico.registrarLiberacao(conteiner.getIdentificador(), liberacao);

        RegistroTransferenciaDTO transferencia = new RegistroTransferenciaDTO();
        transferencia.setPosicaoDestino("P2-C");
        transferencia.setMotivo("Ajuste logístico");
        transferencia.setResponsavel("Supervisor");

        ResponseStatusException excecao = assertThrows(ResponseStatusException.class,
                () -> conteinerServico.registrarTransferencia(conteiner.getIdentificador(), transferencia));

        assertThat(excecao.getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }
}
