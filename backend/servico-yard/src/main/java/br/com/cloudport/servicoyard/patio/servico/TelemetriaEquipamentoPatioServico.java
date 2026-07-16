package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.TelemetriaEquipamentoPatioDto;
import br.com.cloudport.servicoyard.patio.dto.TelemetriaEquipamentoPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.TelemetriaEquipamentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.TelemetriaEquipamentoPatioRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetriaEquipamentoPatioServico {

    private final EquipamentoPatioRepositorio equipamentoRepositorio;
    private final TelemetriaEquipamentoPatioRepositorio telemetriaRepositorio;
    private final TelemetriaEquipamentoStreamingServico streamingServico;

    public TelemetriaEquipamentoPatioServico(
            EquipamentoPatioRepositorio equipamentoRepositorio,
            TelemetriaEquipamentoPatioRepositorio telemetriaRepositorio,
            TelemetriaEquipamentoStreamingServico streamingServico
    ) {
        this.equipamentoRepositorio = equipamentoRepositorio;
        this.telemetriaRepositorio = telemetriaRepositorio;
        this.streamingServico = streamingServico;
    }

    @Transactional
    public TelemetriaEquipamentoPatioDto registrar(
            String identificador,
            TelemetriaEquipamentoPatioRequisicaoDto requisicao
    ) {
        EquipamentoPatio equipamento = equipamentoRepositorio.findByIdentificador(normalizarIdentificador(identificador))
                .orElseThrow(() -> new IllegalArgumentException("Equipamento de patio nao encontrado: " + identificador + "."));
        TelemetriaEquipamentoPatio telemetria = telemetriaRepositorio.findByEquipamentoId(equipamento.getId())
                .orElseGet(TelemetriaEquipamentoPatio::new);
        validarOrdem(telemetria, requisicao);

        telemetria.setEquipamento(equipamento);
        telemetria.setLatitude(requisicao.getLatitude());
        telemetria.setLongitude(requisicao.getLongitude());
        telemetria.setCoordenadaX(requisicao.getCoordenadaX());
        telemetria.setCoordenadaY(requisicao.getCoordenadaY());
        telemetria.setHeading(requisicao.getHeading());
        telemetria.setPosicaoMaisProxima(limpar(requisicao.getPosicaoMaisProxima()));
        telemetria.setDistanciaPosicaoCentimetros(requisicao.getDistanciaPosicaoCentimetros());
        telemetria.setDentroDaPosicao(requisicao.getDentroDaPosicao());
        telemetria.setOrigem(requisicao.getOrigem().trim().toUpperCase(Locale.ROOT));
        telemetria.setOperadorVmt(limpar(requisicao.getOperadorVmt()));
        telemetria.setStatusVmt(normalizarOpcional(requisicao.getStatusVmt()));
        telemetria.setWorkInstructionAtualId(requisicao.getWorkInstructionAtualId());
        telemetria.setSequencia(requisicao.getSequencia());
        telemetria.setCapturadoEm(requisicao.getCapturadoEm());

        if (requisicao.getLinha() != null && requisicao.getColuna() != null) {
            equipamento.setLinha(requisicao.getLinha());
            equipamento.setColuna(requisicao.getColuna());
            equipamentoRepositorio.save(equipamento);
        }

        TelemetriaEquipamentoPatioDto resposta = TelemetriaEquipamentoPatioDto.de(telemetriaRepositorio.save(telemetria));
        streamingServico.publicar(resposta);
        return resposta;
    }

    @Transactional(readOnly = true)
    public List<TelemetriaEquipamentoPatioDto> listar() {
        return telemetriaRepositorio.findAllByOrderByEquipamentoIdentificadorAsc().stream()
                .map(TelemetriaEquipamentoPatioDto::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public TelemetriaEquipamentoPatioDto detalhar(String identificador) {
        return telemetriaRepositorio.findByEquipamentoIdentificador(normalizarIdentificador(identificador))
                .map(TelemetriaEquipamentoPatioDto::de)
                .orElseThrow(() -> new IllegalArgumentException("Telemetria nao encontrada para o equipamento " + identificador + "."));
    }

    private void validarOrdem(TelemetriaEquipamentoPatio atual, TelemetriaEquipamentoPatioRequisicaoDto nova) {
        if (atual.getId() == null) {
            return;
        }
        if (atual.getSequencia() != null && nova.getSequencia() <= atual.getSequencia()) {
            throw new IllegalArgumentException("A sequencia da telemetria deve ser maior que a ultima sequencia processada.");
        }
        LocalDateTime capturadoAtual = atual.getCapturadoEm();
        if (capturadoAtual != null && !nova.getCapturadoEm().isAfter(capturadoAtual)) {
            throw new IllegalArgumentException("A telemetria recebida e anterior ou igual a ultima leitura processada.");
        }
    }

    private String normalizarIdentificador(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Identificador do equipamento e obrigatorio.");
        }
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toUpperCase(Locale.ROOT);
    }

    private String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
