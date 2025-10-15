package br.com.cloudport.servicogate.app.cidadao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.JanelaAtendimento;
import br.com.cloudport.servicogate.model.Motorista;
import br.com.cloudport.servicogate.model.Transportadora;
import br.com.cloudport.servicogate.model.Veiculo;
import br.com.cloudport.servicogate.model.enums.CanalEntrada;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import br.com.cloudport.servicogate.app.transparencia.DashboardMetricsProjection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class AgendamentoRepositoryTest {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Transportadora transportadoraA;
    private Transportadora transportadoraB;

    @BeforeEach
    void setUp() {
        transportadoraA = criarTransportadora("TransA", "001");
        transportadoraB = criarTransportadora("TransB", "002");

        Motorista motoristaA = criarMotorista("João", "123", transportadoraA);
        Motorista motoristaB = criarMotorista("Maria", "456", transportadoraB);

        Veiculo veiculoA = criarVeiculo("AAA1234", transportadoraA);
        Veiculo veiculoB = criarVeiculo("BBB5678", transportadoraB);

        JanelaAtendimento janela1 = criarJanela(LocalDate.of(2024, 1, 10), LocalTime.of(8, 0), LocalTime.of(9, 0), 2);
        JanelaAtendimento janela2 = criarJanela(LocalDate.of(2024, 1, 10), LocalTime.of(10, 0), LocalTime.of(11, 0), 1);
        JanelaAtendimento janela3 = criarJanela(LocalDate.of(2024, 1, 10), LocalTime.of(12, 0), LocalTime.of(13, 0), 1);
        JanelaAtendimento janela4 = criarJanela(LocalDate.of(2024, 1, 10), LocalTime.of(14, 0), LocalTime.of(15, 0), 1);
        JanelaAtendimento janela5 = criarJanela(LocalDate.of(2024, 1, 11), LocalTime.of(9, 0), LocalTime.of(10, 0), 1);

        criarAgendamento("AG001", transportadoraA, motoristaA, veiculoA, janela1,
                TipoOperacao.ENTRADA, StatusAgendamento.COMPLETO,
                LocalDateTime.of(2024, 1, 10, 8, 0), LocalDateTime.of(2024, 1, 10, 9, 0),
                LocalDateTime.of(2024, 1, 10, 8, 5), LocalDateTime.of(2024, 1, 10, 8, 55));

        criarAgendamento("AG002", transportadoraA, motoristaA, veiculoA, janela2,
                TipoOperacao.ENTRADA, StatusAgendamento.COMPLETO,
                LocalDateTime.of(2024, 1, 10, 10, 0), LocalDateTime.of(2024, 1, 10, 11, 30),
                LocalDateTime.of(2024, 1, 10, 10, 30), LocalDateTime.of(2024, 1, 10, 11, 45));

        criarAgendamento("AG003", transportadoraA, motoristaA, veiculoA, janela3,
                TipoOperacao.ENTRADA, StatusAgendamento.NO_SHOW,
                LocalDateTime.of(2024, 1, 10, 12, 0), LocalDateTime.of(2024, 1, 10, 13, 0),
                null, null);

        criarAgendamento("AG004", transportadoraA, motoristaA, veiculoA, janela4,
                TipoOperacao.ENTRADA, StatusAgendamento.CANCELADO,
                LocalDateTime.of(2024, 1, 10, 14, 0), LocalDateTime.of(2024, 1, 10, 15, 0),
                null, null);

        criarAgendamento("AG005", transportadoraB, motoristaB, veiculoB, janela5,
                TipoOperacao.SAIDA, StatusAgendamento.COMPLETO,
                LocalDateTime.of(2024, 1, 11, 9, 0), LocalDateTime.of(2024, 1, 11, 10, 0),
                LocalDateTime.of(2024, 1, 11, 9, 5), LocalDateTime.of(2024, 1, 11, 9, 50));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deveCalcularMetricasDashboard() {
        LocalDateTime inicio = LocalDateTime.of(2024, 1, 10, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2024, 1, 10, 23, 59);

        DashboardMetricsProjection projection = agendamentoRepository.calcularMetricasDashboard(
                inicio,
                fim,
                transportadoraA.getId(),
                TipoOperacao.ENTRADA.name(),
                15
        );

        assertThat(projection.getTotalAgendamentos()).isEqualTo(3);
        assertThat(projection.getPontuais()).isEqualTo(1);
        assertThat(projection.getNoShow()).isEqualTo(1);
        assertThat(projection.getTurnaroundMedio()).isCloseTo(62.5, within(0.1));
        assertThat(projection.getOcupacaoSlots()).isCloseTo(0.75, within(0.0001));
    }

    @Test
    void deveBuscarAgendamentosParaRelatorioComFiltros() {
        LocalDateTime inicio = LocalDateTime.of(2024, 1, 10, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2024, 1, 10, 23, 59);

        List<Agendamento> resultados = agendamentoRepository.buscarRelatorio(
                inicio,
                fim,
                transportadoraA.getId(),
                TipoOperacao.ENTRADA
        );

        assertThat(resultados)
                .extracting(Agendamento::getCodigo)
                .containsExactly("AG001", "AG002", "AG003");
        assertThat(resultados)
                .allMatch(agendamento -> agendamento.getTransportadora().getId().equals(transportadoraA.getId()));
        assertThat(resultados)
                .allMatch(agendamento -> agendamento.getTipoOperacao() == TipoOperacao.ENTRADA);
    }

    private Transportadora criarTransportadora(String nome, String documento) {
        Transportadora transportadora = new Transportadora();
        transportadora.setNome(nome);
        transportadora.setDocumento(documento);
        return entityManager.persist(transportadora);
    }

    private Motorista criarMotorista(String nome, String documento, Transportadora transportadora) {
        Motorista motorista = new Motorista();
        motorista.setNome(nome);
        motorista.setDocumento(documento);
        motorista.setTransportadora(transportadora);
        return entityManager.persist(motorista);
    }

    private Veiculo criarVeiculo(String placa, Transportadora transportadora) {
        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca(placa);
        veiculo.setTransportadora(transportadora);
        return entityManager.persist(veiculo);
    }

    private JanelaAtendimento criarJanela(LocalDate data, LocalTime inicio, LocalTime fim, int capacidade) {
        JanelaAtendimento janela = new JanelaAtendimento();
        janela.setData(data);
        janela.setHoraInicio(inicio);
        janela.setHoraFim(fim);
        janela.setCapacidade(capacidade);
        janela.setCanalEntrada(CanalEntrada.PORTARIA_PRINCIPAL);
        return entityManager.persist(janela);
    }

    private Agendamento criarAgendamento(String codigo,
                                         Transportadora transportadora,
                                         Motorista motorista,
                                         Veiculo veiculo,
                                         JanelaAtendimento janela,
                                         TipoOperacao tipoOperacao,
                                         StatusAgendamento status,
                                         LocalDateTime previstoChegada,
                                         LocalDateTime previstoSaida,
                                         LocalDateTime realChegada,
                                         LocalDateTime realSaida) {
        Agendamento agendamento = new Agendamento();
        agendamento.setCodigo(codigo);
        agendamento.setTransportadora(transportadora);
        agendamento.setMotorista(motorista);
        agendamento.setVeiculo(veiculo);
        agendamento.setJanelaAtendimento(janela);
        agendamento.setTipoOperacao(tipoOperacao);
        agendamento.setStatus(status);
        agendamento.setHorarioPrevistoChegada(previstoChegada);
        agendamento.setHorarioPrevistoSaida(previstoSaida);
        agendamento.setHorarioRealChegada(realChegada);
        agendamento.setHorarioRealSaida(realSaida);
        return entityManager.persist(agendamento);
    }
}
