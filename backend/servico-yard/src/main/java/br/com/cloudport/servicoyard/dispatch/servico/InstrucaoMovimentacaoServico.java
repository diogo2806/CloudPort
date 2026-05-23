package br.com.cloudport.servicoyard.dispatch.servico;

import br.com.cloudport.servicoyard.container.validacao.SanitizadorEntrada;
import br.com.cloudport.servicoyard.dispatch.dto.CadastroInstrucaoMovimentacaoDTO;
import br.com.cloudport.servicoyard.dispatch.dto.DespachoInstrucaoDTO;
import br.com.cloudport.servicoyard.dispatch.dto.InstrucaoMovimentacaoDTO;
import br.com.cloudport.servicoyard.dispatch.modelo.InstrucaoMovimentacao;
import br.com.cloudport.servicoyard.dispatch.modelo.StatusInstrucaoMovimentacao;
import br.com.cloudport.servicoyard.dispatch.repositorio.InstrucaoMovimentacaoRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InstrucaoMovimentacaoServico {

    private final InstrucaoMovimentacaoRepositorio instrucaoRepositorio;
    private final EquipamentoPatioRepositorio equipamentoRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public InstrucaoMovimentacaoServico(InstrucaoMovimentacaoRepositorio instrucaoRepositorio,
                                        EquipamentoPatioRepositorio equipamentoRepositorio,
                                        SanitizadorEntrada sanitizadorEntrada) {
        this.instrucaoRepositorio = instrucaoRepositorio;
        this.equipamentoRepositorio = equipamentoRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Transactional(readOnly = true)
    public List<InstrucaoMovimentacaoDTO> listar() {
        return instrucaoRepositorio.findAllByOrderBySequenciaAscCriadoEmAsc().stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InstrucaoMovimentacaoDTO buscar(Long id) {
        return mapear(obter(id));
    }

    @Transactional(readOnly = true)
    public List<InstrucaoMovimentacaoDTO> jobListEquipamento(Long equipamentoId) {
        return instrucaoRepositorio
                .findByEquipamentoIdAndStatusInOrderByPrioridadeFetchDescSequenciaAscCriadoEmAsc(
                        equipamentoId,
                        Arrays.asList(StatusInstrucaoMovimentacao.DESPACHADA, StatusInstrucaoMovimentacao.EM_EXECUCAO))
                .stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Transactional
    public InstrucaoMovimentacaoDTO planejar(CadastroInstrucaoMovimentacaoDTO dto) {
        InstrucaoMovimentacao instrucao = new InstrucaoMovimentacao();
        instrucao.setCodigoConteiner(limparObrigatorio(dto.getCodigoConteiner(), "código do contêiner").toUpperCase(Locale.ROOT));
        instrucao.setTipoMove(dto.getTipoMove());
        instrucao.setPosicaoOrigem(sanitizadorEntrada.limparTexto(dto.getPosicaoOrigem()));
        instrucao.setPosicaoDestino(sanitizadorEntrada.limparTexto(dto.getPosicaoDestino()));
        instrucao.setIsoTipo(sanitizadorEntrada.limparTexto(dto.getIsoTipo()));
        instrucao.setComprimentoPes(dto.getComprimentoPes());
        instrucao.setLineOperator(sanitizadorEntrada.limparTexto(dto.getLineOperator()));
        instrucao.setPortoOrigem(sanitizadorEntrada.limparTexto(dto.getPortoOrigem()));
        instrucao.setPortoDestino(sanitizadorEntrada.limparTexto(dto.getPortoDestino()));
        instrucao.setPesoKg(dto.getPesoKg());
        instrucao.setFilaTrabalho(sanitizadorEntrada.limparTexto(dto.getFilaTrabalho()));
        instrucao.setSequencia(dto.getSequencia() != null ? dto.getSequencia() : 0);
        instrucao.setPrioridadeFetch(dto.isPrioridadeFetch());
        instrucao.setMoveTwin(dto.isMoveTwin());
        instrucao.setRequerEnergia(dto.isRequerEnergia());
        instrucao.setPerigoso(dto.isPerigoso());
        instrucao.setForaDeBitola(dto.isForaDeBitola());
        instrucao.setStatus(StatusInstrucaoMovimentacao.PLANEJADA);
        LocalDateTime agora = LocalDateTime.now();
        instrucao.setCriadoEm(agora);
        instrucao.setAtualizadoEm(agora);
        return mapear(instrucaoRepositorio.save(instrucao));
    }

    @Transactional
    public InstrucaoMovimentacaoDTO despachar(Long id, DespachoInstrucaoDTO dto) {
        InstrucaoMovimentacao instrucao = obter(id);
        if (instrucao.getStatus() != StatusInstrucaoMovimentacao.PLANEJADA
                && instrucao.getStatus() != StatusInstrucaoMovimentacao.DESPACHADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Só é possível despachar instruções planejadas ou já despachadas.");
        }
        EquipamentoPatio equipamento = equipamentoRepositorio.findById(dto.getEquipamentoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipamento (CHE) não encontrado."));
        instrucao.setEquipamento(equipamento);
        instrucao.setStatus(StatusInstrucaoMovimentacao.DESPACHADA);
        if (dto.getSequencia() != null) {
            instrucao.setSequencia(dto.getSequencia());
        }
        instrucao.setAtualizadoEm(LocalDateTime.now());
        return mapear(instrucaoRepositorio.save(instrucao));
    }

    @Transactional
    public InstrucaoMovimentacaoDTO iniciar(Long id) {
        InstrucaoMovimentacao instrucao = obter(id);
        if (instrucao.getStatus() != StatusInstrucaoMovimentacao.DESPACHADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A instrução precisa estar despachada a um equipamento para iniciar a execução.");
        }
        instrucao.setStatus(StatusInstrucaoMovimentacao.EM_EXECUCAO);
        instrucao.setAtualizadoEm(LocalDateTime.now());
        return mapear(instrucaoRepositorio.save(instrucao));
    }

    @Transactional
    public InstrucaoMovimentacaoDTO concluir(Long id) {
        InstrucaoMovimentacao instrucao = obter(id);
        if (instrucao.getStatus() != StatusInstrucaoMovimentacao.EM_EXECUCAO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Só é possível concluir uma instrução em execução.");
        }
        instrucao.setStatus(StatusInstrucaoMovimentacao.CONCLUIDA);
        LocalDateTime agora = LocalDateTime.now();
        instrucao.setConcluidoEm(agora);
        instrucao.setAtualizadoEm(agora);
        return mapear(instrucaoRepositorio.save(instrucao));
    }

    @Transactional
    public InstrucaoMovimentacaoDTO cancelar(Long id) {
        InstrucaoMovimentacao instrucao = obter(id);
        if (instrucao.getStatus() == StatusInstrucaoMovimentacao.CONCLUIDA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Uma instrução concluída não pode ser cancelada.");
        }
        instrucao.setStatus(StatusInstrucaoMovimentacao.CANCELADA);
        instrucao.setAtualizadoEm(LocalDateTime.now());
        return mapear(instrucaoRepositorio.save(instrucao));
    }

    private InstrucaoMovimentacao obter(Long id) {
        return instrucaoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrução de movimentação não encontrada."));
    }

    private String limparObrigatorio(String valor, String descricaoCampo) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        if (!StringUtils.hasText(limpo)) {
            throw new IllegalArgumentException("O campo " + descricaoCampo + " é obrigatório e possui caracteres inválidos.");
        }
        return limpo;
    }

    private InstrucaoMovimentacaoDTO mapear(InstrucaoMovimentacao instrucao) {
        EquipamentoPatio equipamento = instrucao.getEquipamento();
        return new InstrucaoMovimentacaoDTO(
                instrucao.getId(),
                instrucao.getCodigoConteiner(),
                instrucao.getIsoTipo(),
                instrucao.getComprimentoPes(),
                instrucao.getLineOperator(),
                instrucao.getPortoOrigem(),
                instrucao.getPortoDestino(),
                instrucao.getPesoKg(),
                instrucao.getTipoMove(),
                instrucao.getPosicaoOrigem(),
                instrucao.getPosicaoDestino(),
                equipamento != null ? equipamento.getId() : null,
                equipamento != null ? equipamento.getIdentificador() : null,
                instrucao.getFilaTrabalho(),
                instrucao.getSequencia(),
                instrucao.isPrioridadeFetch(),
                instrucao.isMoveTwin(),
                instrucao.isRequerEnergia(),
                instrucao.isPerigoso(),
                instrucao.isForaDeBitola(),
                instrucao.getStatus(),
                instrucao.getCriadoEm(),
                instrucao.getAtualizadoEm(),
                instrucao.getConcluidoEm()
        );
    }
}
