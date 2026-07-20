package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.OperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dto.OrdemLiberacaoStuffUnstuffDTOs.OrigemOperacionalRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CancelarOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.ConcluirOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CriarOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.OperacaoResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.RegistrarExecucaoRequest;
import br.com.cloudport.servicocargageral.repositorio.OperacaoStuffUnstuffRepositorio;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FluxoStuffUnstuffServico {

    private final StuffUnstuffServico operacaoServico;
    private final PlanoStuffUnstuffServico planoServico;
    private final ProgramacaoDocaCargaServico programacaoDocaServico;
    private final OrdemLiberacaoStuffUnstuffServico ordemLiberacaoServico;
    private final OperacaoStuffUnstuffRepositorio operacaoRepositorio;

    public FluxoStuffUnstuffServico(
            StuffUnstuffServico operacaoServico,
            PlanoStuffUnstuffServico planoServico,
            ProgramacaoDocaCargaServico programacaoDocaServico,
            OrdemLiberacaoStuffUnstuffServico ordemLiberacaoServico,
            OperacaoStuffUnstuffRepositorio operacaoRepositorio) {
        this.operacaoServico = operacaoServico;
        this.planoServico = planoServico;
        this.programacaoDocaServico = programacaoDocaServico;
        this.ordemLiberacaoServico = ordemLiberacaoServico;
        this.operacaoRepositorio = operacaoRepositorio;
    }

    @Transactional
    public OperacaoResposta criar(CriarOperacaoRequest request, OrigemOperacionalRequest origemOperacional) {
        OperacaoResposta criada = operacaoServico.criarOperacaoStuffUnstuff(request);
        OperacaoStuffUnstuff operacao = operacaoRepositorio.findDetalhadaById(criada.id())
                .orElseThrow(() -> naoEncontrada("Operação criada não encontrada."));
        BigDecimal quantidadePlanejada = request.itens().stream()
                .map(item -> item.quantidadePlanejada())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ordemLiberacaoServico.reservar(operacao.getId(), origemOperacional, quantidadePlanejada);
        planoServico.criarVersaoInicial(operacao, request.usuario());
        return operacaoServico.obter(criada.id());
    }

    @Transactional
    public OperacaoResposta criar(CriarOperacaoRequest request) {
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A origem operacional é obrigatória para criar operação de stuff/unstuff.");
    }

    @Transactional
    public OperacaoResposta iniciar(UUID id, String usuario, String correlationId) {
        ordemLiberacaoServico.validarParaInicio(id);
        planoServico.exigirPlanoLiberado(id);
        programacaoDocaServico.iniciarParaOperacao(id, usuario, correlationId);
        return operacaoServico.iniciar(id, usuario, correlationId);
    }

    @Transactional
    public OperacaoResposta registrarExecucao(UUID id, RegistrarExecucaoRequest request) {
        planoServico.exigirPlanoLiberado(id);
        programacaoDocaServico.exigirEmUso(id);
        OperacaoResposta resposta = operacaoServico.registrarExecucao(id, request);
        ordemLiberacaoServico.consumir(id, request.commandId(), request.quantidade());
        return resposta;
    }

    @Transactional
    public OperacaoResposta concluir(UUID id, ConcluirOperacaoRequest request) {
        programacaoDocaServico.exigirEmUso(id);
        ordemLiberacaoServico.concluir(id);
        operacaoServico.concluir(id, request);
        programacaoDocaServico.concluirParaOperacao(id, request.usuario(), request.correlationId());
        return operacaoServico.obter(id);
    }

    @Transactional
    public OperacaoResposta cancelar(UUID id, CancelarOperacaoRequest request) {
        programacaoDocaServico.cancelarParaOperacao(
                id,
                request.usuario(),
                request.correlationId(),
                request.motivo());
        OperacaoResposta resposta = operacaoServico.cancelar(id, request);
        ordemLiberacaoServico.compensarCancelamento(id);
        return resposta;
    }

    private ResponseStatusException naoEncontrada(String mensagem) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
    }
}
