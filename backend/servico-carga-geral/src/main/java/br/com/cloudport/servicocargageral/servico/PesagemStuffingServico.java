package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.OperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.ConfirmarPesagemStuffingRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.PesagemStuffingResposta;
import br.com.cloudport.servicocargageral.repositorio.OperacaoStuffUnstuffRepositorio;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PesagemStuffingServico {

    private final OperacaoStuffUnstuffRepositorio operacaoRepositorio;

    public PesagemStuffingServico(OperacaoStuffUnstuffRepositorio operacaoRepositorio) {
        this.operacaoRepositorio = operacaoRepositorio;
    }

    @Transactional(readOnly = true)
    public PesagemStuffingResposta obter(UUID operacaoId) {
        OperacaoStuffUnstuff operacao = operacaoRepositorio.findDetalhadaById(operacaoId)
                .orElseThrow(() -> naoEncontrada("Operação de stuff/unstuff não encontrada."));
        return mapear(operacao);
    }

    @Transactional
    public PesagemStuffingResposta confirmar(UUID operacaoId, ConfirmarPesagemStuffingRequest request) {
        OperacaoStuffUnstuff operacao = operacaoRepositorio.findComBloqueioById(operacaoId)
                .orElseThrow(() -> naoEncontrada("Operação de stuff/unstuff não encontrada."));
        try {
            operacao.confirmarPesagemStuffing(
                    request.metodoPesagem(),
                    request.taraKg(),
                    request.pesoBrutoKg(),
                    request.vgmKg(),
                    request.capacidadeMaximaKg(),
                    request.equipamentoPesagem(),
                    request.responsavelPesagem(),
                    request.usuario(),
                    request.correlationId(),
                    request.observacao());
        } catch (IllegalStateException exception) {
            throw conflito(exception.getMessage());
        }
        return mapear(operacaoRepositorio.save(operacao));
    }

    private PesagemStuffingResposta mapear(OperacaoStuffUnstuff operacao) {
        return new PesagemStuffingResposta(
                operacao.getId(),
                operacao.getConteinerId(),
                operacao.getMetodoPesagem(),
                operacao.getStatusPesagemVgm(),
                operacao.getTaraKg(),
                operacao.getPesoBrutoKg(),
                operacao.getVgmKg(),
                operacao.getCapacidadeMaximaKg(),
                operacao.getEquipamentoPesagem(),
                operacao.getResponsavelPesagem(),
                operacao.getPesagemConfirmadaEm(),
                operacao.getMotivoBloqueioPeso(),
                operacao.possuiPesagemLiberada());
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }

    private ResponseStatusException naoEncontrada(String mensagem) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
    }
}
