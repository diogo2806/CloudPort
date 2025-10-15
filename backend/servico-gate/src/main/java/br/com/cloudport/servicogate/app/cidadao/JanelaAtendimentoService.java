package br.com.cloudport.servicogate.app.cidadao;

import br.com.cloudport.servicogate.app.cidadao.dto.JanelaAtendimentoDTO;
import br.com.cloudport.servicogate.app.cidadao.dto.JanelaAtendimentoRequest;
import br.com.cloudport.servicogate.app.cidadao.dto.mapper.GateMapper;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.NotFoundException;
import br.com.cloudport.servicogate.model.JanelaAtendimento;
import br.com.cloudport.servicogate.model.enums.CanalEntrada;
import br.com.cloudport.servicogate.app.cidadao.JanelaAtendimentoRepository;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class JanelaAtendimentoService {

    private final JanelaAtendimentoRepository janelaAtendimentoRepository;

    public JanelaAtendimentoService(JanelaAtendimentoRepository janelaAtendimentoRepository) {
        this.janelaAtendimentoRepository = janelaAtendimentoRepository;
    }

    @Transactional(readOnly = true)
    public Page<JanelaAtendimentoDTO> buscar(LocalDate dataInicio, LocalDate dataFim, Pageable pageable) {
        Page<JanelaAtendimento> page;
        if (dataInicio != null && dataFim != null) {
            page = janelaAtendimentoRepository.findByDataBetween(dataInicio, dataFim, pageable);
        } else if (dataInicio != null) {
            page = janelaAtendimentoRepository.findByDataGreaterThanEqual(dataInicio, pageable);
        } else if (dataFim != null) {
            page = janelaAtendimentoRepository.findByDataLessThanEqual(dataFim, pageable);
        } else {
            page = janelaAtendimentoRepository.findAll(pageable);
        }
        return page.map(GateMapper::toJanelaAtendimentoDTO);
    }

    @Transactional(readOnly = true)
    public JanelaAtendimentoDTO buscarPorId(Long id) {
        JanelaAtendimento janela = janelaAtendimentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Janela de atendimento não encontrada"));
        return GateMapper.toJanelaAtendimentoDTO(janela);
    }

    public JanelaAtendimentoDTO criar(JanelaAtendimentoRequest request) {
        validarIntervaloHorarios(request);
        JanelaAtendimento janela = new JanelaAtendimento();
        aplicarDados(janela, request);
        JanelaAtendimento salvo = janelaAtendimentoRepository.save(janela);
        return GateMapper.toJanelaAtendimentoDTO(salvo);
    }

    public JanelaAtendimentoDTO atualizar(Long id, JanelaAtendimentoRequest request) {
        validarIntervaloHorarios(request);
        JanelaAtendimento existente = janelaAtendimentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Janela de atendimento não encontrada"));
        aplicarDados(existente, request);
        JanelaAtendimento salvo = janelaAtendimentoRepository.save(existente);
        return GateMapper.toJanelaAtendimentoDTO(salvo);
    }

    public void remover(Long id) {
        JanelaAtendimento existente = janelaAtendimentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Janela de atendimento não encontrada"));
        janelaAtendimentoRepository.delete(existente);
    }

    private void validarIntervaloHorarios(JanelaAtendimentoRequest request) {
        if (request.getHoraFim().isBefore(request.getHoraInicio()) || request.getHoraFim().equals(request.getHoraInicio())) {
            throw new BusinessException("Hora fim deve ser posterior à hora início");
        }
    }

    private void aplicarDados(JanelaAtendimento janela, JanelaAtendimentoRequest request) {
        janela.setData(request.getData());
        janela.setHoraInicio(request.getHoraInicio());
        janela.setHoraFim(request.getHoraFim());
        janela.setCapacidade(request.getCapacidade());
        janela.setCanalEntrada(parseCanalEntrada(request.getCanalEntrada()));
    }

    private CanalEntrada parseCanalEntrada(String canal) {
        try {
            String normalized = canal.trim().toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
            return CanalEntrada.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Canal de entrada inválido: " + canal);
        }
    }
}
