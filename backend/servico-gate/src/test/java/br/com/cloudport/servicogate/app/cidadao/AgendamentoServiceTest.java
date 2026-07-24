package br.com.cloudport.servicogate.app.cidadao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.app.configuracoes.TransportadoraRepository;
import br.com.cloudport.servicogate.app.transparencia.DashboardService;
import br.com.cloudport.servicogate.app.cidadao.dto.AgendamentoDTO;
import br.com.cloudport.servicogate.app.cidadao.dto.AgendamentoRequest;
import br.com.cloudport.servicogate.config.AgendamentoRulesProperties;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.integration.ocr.ProcessamentoOcrPublisher;
import br.com.cloudport.servicogate.integration.tos.TosIntegrationService;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.JanelaAtendimento;
import br.com.cloudport.servicogate.model.Motorista;
import br.com.cloudport.servicogate.model.Transportadora;
import br.com.cloudport.servicogate.model.Veiculo;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.storage.DocumentoStorageService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;
    @Mock
    private JanelaAtendimentoRepository janelaAtendimentoRepository;
    @Mock
    private TransportadoraRepository transportadoraRepository;
    @Mock
    private MotoristaRepository motoristaRepository;
    @Mock
    private VeiculoRepository veiculoRepository;
    @Mock
    private DocumentoAgendamentoRepository documentoAgendamentoRepository;
    @Mock
    private DocumentoStorageService documentoStorageService;
    @Mock
    private TosIntegrationService tosIntegrationService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private AgendamentoRealtimeService agendamentoRealtimeService;
    @Mock
    private ProcessamentoOcrPublisher processamentoOcrPublisher;

    private AgendamentoRulesProperties rulesProperties;
    private AgendamentoService agendamentoService;

    @BeforeEach
    void setUp() {
        rulesProperties = new AgendamentoRulesProperties();
        agendamentoService = new AgendamentoService(
                agendamentoRepository,
                janelaAtendimentoRepository,
                transportadoraRepository,
                motoristaRepository,
                veiculoRepository,
                documentoAgendamentoRepository,
                documentoStorageService,
                rulesProperties,
                tosIntegrationService,
                dashboardService,
                agendamentoRealtimeService,
                processamentoOcrPublisher
        );
    }

    @Test
    @DisplayName("Deve impedir criação quando capacidade da janela é excedida")
    void deveImpedirCriacaoQuandoCapacidadeAtingida() {
        JanelaAtendimento janela = criarJanelaComCapacidade(1);
        when(janelaAtendimentoRepository.findById(1L)).thenReturn(Optional.of(janela));
        when(agendamentoRepository.countByJanelaAtendimentoIdAndStatusNot(janela.getId(), StatusAgendamento.CANCELADO))
                .thenReturn(1L);

        AgendamentoRequest request = criarRequestBasico();

        assertThatThrownBy(() -> agendamentoService.criar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Capacidade da janela atingida");

        verify(agendamentoRepository, never()).save(any());
        verify(tosIntegrationService, never()).validarAgendamentoParaCriacao(any(), any());
    }

    @Test
    @DisplayName("Deve criar agendamento com veículo ativo da transportadora")
    void deveCriarAgendamento() {
        Transportadora transportadora = criarTransportadora(2L);
        Veiculo veiculo = criarVeiculo(4L, transportadora, true);
        prepararCriacaoValida(transportadora, veiculo);
        when(agendamentoRepository.save(any())).thenAnswer(invocation -> {
            Agendamento entity = invocation.getArgument(0);
            entity.setId(99L);
            return entity;
        });

        AgendamentoDTO dto = agendamentoService.criar(criarRequestBasico());

        ArgumentCaptor<Agendamento> captor = ArgumentCaptor.forClass(Agendamento.class);
        verify(agendamentoRepository).save(captor.capture());
        verify(tosIntegrationService).validarAgendamentoParaCriacao("AG001", any());
        verify(dashboardService).publicarResumoGeral();
        verify(agendamentoRealtimeService).notificarStatus(any());

        Agendamento salvo = captor.getValue();
        assertThat(salvo.getCodigo()).isEqualTo("AG001");
        assertThat(salvo.getVeiculo()).isEqualTo(veiculo);
        assertThat(dto.getId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("Deve bloquear veículo inativo em novo agendamento")
    void deveBloquearVeiculoInativo() {
        Transportadora transportadora = criarTransportadora(2L);
        prepararCriacaoValida(transportadora, criarVeiculo(4L, transportadora, false));

        assertThatThrownBy(() -> agendamentoService.criar(criarRequestBasico()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Veículo inativo");

        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve bloquear veículo pertencente a outra transportadora")
    void deveBloquearVeiculoDeOutraTransportadora() {
        Transportadora transportadora = criarTransportadora(2L);
        Transportadora outra = criarTransportadora(9L);
        prepararCriacaoValida(transportadora, criarVeiculo(4L, outra, true));

        assertThatThrownBy(() -> agendamentoService.criar(criarRequestBasico()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pertence à transportadora");

        verify(agendamentoRepository, never()).save(any());
    }

    private void prepararCriacaoValida(Transportadora transportadora, Veiculo veiculo) {
        JanelaAtendimento janela = criarJanelaComCapacidade(5);
        when(janelaAtendimentoRepository.findById(1L)).thenReturn(Optional.of(janela));
        when(transportadoraRepository.findById(2L)).thenReturn(Optional.of(transportadora));
        when(motoristaRepository.findById(3L)).thenReturn(Optional.of(new Motorista()));
        when(veiculoRepository.findById(4L)).thenReturn(Optional.of(veiculo));
        when(agendamentoRepository.countByJanelaAtendimentoIdAndStatusNot(janela.getId(), StatusAgendamento.CANCELADO))
                .thenReturn(0L);
    }

    private Transportadora criarTransportadora(Long id) {
        Transportadora transportadora = new Transportadora();
        transportadora.setId(id);
        transportadora.setNome("Transportadora " + id);
        return transportadora;
    }

    private Veiculo criarVeiculo(Long id, Transportadora transportadora, boolean ativo) {
        Veiculo veiculo = new Veiculo();
        veiculo.setId(id);
        veiculo.setPlaca("ABC1D23");
        veiculo.setTransportadora(transportadora);
        veiculo.setAtivo(ativo);
        return veiculo;
    }

    private JanelaAtendimento criarJanelaComCapacidade(int capacidade) {
        JanelaAtendimento janela = new JanelaAtendimento();
        janela.setId(1L);
        janela.setCapacidade(capacidade);
        janela.setData(LocalDate.now().plusDays(1));
        janela.setHoraInicio(LocalTime.of(10, 0));
        janela.setHoraFim(LocalTime.of(14, 0));
        return janela;
    }

    private AgendamentoRequest criarRequestBasico() {
        LocalDateTime inicio = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(11, 0));
        AgendamentoRequest request = new AgendamentoRequest();
        request.setCodigo("AG001");
        request.setTipoOperacao("ENTRADA");
        request.setStatus("PENDENTE");
        request.setTransportadoraId(2L);
        request.setMotoristaId(3L);
        request.setVeiculoId(4L);
        request.setJanelaAtendimentoId(1L);
        request.setHorarioPrevistoChegada(inicio);
        request.setHorarioPrevistoSaida(inicio.plusHours(1));
        request.setObservacoes("Teste automatizado");
        return request;
    }
}
