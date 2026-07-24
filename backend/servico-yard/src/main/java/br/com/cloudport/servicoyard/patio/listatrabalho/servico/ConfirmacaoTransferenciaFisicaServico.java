package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.patio.avisoestivagem.servico.AvisoEstivagemPatioServico;
import br.com.cloudport.servicoyard.patio.dto.ConteinerPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.EventoVmtWorkInstructionRequest;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.TipoAcaoFisicaPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.servico.MapaPatioServico;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConfirmacaoTransferenciaFisicaServico {

    private final WorkQueuePatioRepositorio workQueueRepositorio;
    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final EquipamentoPatioRepositorio equipamentoRepositorio;
    private final ConteinerPatioRepositorio conteinerRepositorio;
    private final PosicaoPatioRepositorio posicaoRepositorio;
    private final MapaPatioServico mapaPatioServico;
    private final AvisoEstivagemPatioServico avisoEstivagemServico;

    public ConfirmacaoTransferenciaFisicaServico(WorkQueuePatioRepositorio workQueueRepositorio,
                                                  OrdemTrabalhoPatioRepositorio ordemRepositorio,
                                                  EquipamentoPatioRepositorio equipamentoRepositorio,
                                                  ConteinerPatioRepositorio conteinerRepositorio,
                                                  PosicaoPatioRepositorio posicaoRepositorio,
                                                  MapaPatioServico mapaPatioServico,
                                                  AvisoEstivagemPatioServico avisoEstivagemServico) {
        this.workQueueRepositorio = workQueueRepositorio;
        this.ordemRepositorio = ordemRepositorio;
        this.equipamentoRepositorio = equipamentoRepositorio;
        this.conteinerRepositorio = conteinerRepositorio;
        this.posicaoRepositorio = posicaoRepositorio;
        this.mapaPatioServico = mapaPatioServico;
        this.avisoEstivagemServico = avisoEstivagemServico;
    }

    @Transactional
    public void confirmar(OrdemTrabalhoPatio ordem, EventoVmtWorkInstructionRequest request) {
        validarLeituraUnidade(ordem, request);
        WorkQueuePatio fila = validarFila(ordem);
        validarEquipamento(fila, request);
        validarAcaoFisica(ordem, request);
        validarSequencia(ordem, fila, request);

        if (request.getTipoAcaoFisica() == TipoAcaoFisicaPatio.GROUNDING) {
            confirmarGrounding(ordem, request);
        } else {
            confirmarUngrounding(ordem, request);
        }
        avisoEstivagemServico.revalidarInventario("VMT_ORDEM_" + ordem.getId());
    }

    private void validarLeituraUnidade(OrdemTrabalhoPatio ordem,
                                       EventoVmtWorkInstructionRequest request) {
        String unidadeLida = obrigatorio(request.getCodigoUnidadeLido(), "A leitura da unidade");
        if (!unidadeLida.equalsIgnoreCase(ordem.getCodigoConteiner())) {
            throw conflito("A unidade lida nao corresponde a work instruction.");
        }
    }

    private WorkQueuePatio validarFila(OrdemTrabalhoPatio ordem) {
        if (ordem.getWorkQueueId() == null) {
            throw conflito("A work instruction nao possui work queue associada.");
        }
        return workQueueRepositorio.findById(ordem.getWorkQueueId())
                .orElseThrow(() -> conflito("A work queue associada nao foi encontrada."));
    }

    private void validarEquipamento(WorkQueuePatio fila,
                                    EventoVmtWorkInstructionRequest request) {
        if (fila.getEquipamentoPatioId() == null || !StringUtils.hasText(fila.getEquipamento())) {
            throw conflito("A work queue nao possui CHE real associado.");
        }
        if (!Objects.equals(fila.getEquipamentoPatioId(), request.getEquipamentoPatioId())) {
            throw conflito("O CHE informado diverge do equipamento associado a work queue.");
        }
        String identificador = obrigatorio(request.getEquipamentoIdentificador(), "O identificador do CHE");
        EquipamentoPatio equipamento = equipamentoRepositorio.findById(fila.getEquipamentoPatioId())
                .orElseThrow(() -> conflito("O CHE associado a work queue nao foi encontrado."));
        if (!equipamento.getIdentificador().equalsIgnoreCase(identificador)
                || !fila.getEquipamento().equalsIgnoreCase(identificador)) {
            throw conflito("O identificador lido do CHE diverge do equipamento associado.");
        }
        if (equipamento.getStatusOperacional() != StatusEquipamento.OPERACIONAL) {
            throw conflito("O CHE informado nao esta operacional.");
        }
    }

    private void validarAcaoFisica(OrdemTrabalhoPatio ordem,
                                    EventoVmtWorkInstructionRequest request) {
        if (request.getTipoAcaoFisica() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O tipo de acao fisica deve ser informado.");
        }
        TipoAcaoFisicaPatio esperado = tipoAcaoEsperado(ordem.getTipoMovimento());
        if (request.getTipoAcaoFisica() != esperado) {
            throw conflito("A acao fisica " + request.getTipoAcaoFisica()
                    + " nao corresponde ao movimento " + ordem.getTipoMovimento() + ".");
        }
    }

    private void validarSequencia(OrdemTrabalhoPatio ordem,
                                  WorkQueuePatio fila,
                                  EventoVmtWorkInstructionRequest request) {
        if (request.getSequenciaOperacional() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A sequencia operacional deve ser informada.");
        }
        if (ordem.getSequenciaNavio() != null
                && !Objects.equals(ordem.getSequenciaNavio(), request.getSequenciaOperacional())) {
            throw conflito("A sequencia operacional informada diverge da work instruction.");
        }

        List<OrdemTrabalhoPatio> ordens = ordemRepositorio
                .findByWorkQueueIdOrderByPrioridadeBuscaDescPrioridadeOperacionalAscSequenciaNavioAscCriadoEmAsc(
                        fila.getId());
        for (OrdemTrabalhoPatio anterior : ordens) {
            if (Objects.equals(anterior.getId(), ordem.getId())) {
                break;
            }
            if (anterior.getStatusOrdem() != StatusOrdemTrabalhoPatio.CONCLUIDA
                    && anterior.getStatusOrdem() != StatusOrdemTrabalhoPatio.CANCELADA) {
                throw conflito("Existe uma work instruction anterior ainda nao concluida na job list.");
            }
        }
    }

    private void confirmarGrounding(OrdemTrabalhoPatio ordem,
                                     EventoVmtWorkInstructionRequest request) {
        obrigatorio(request.getOrigem(), "A origem fisica");
        validarDestinoDaOrdem(ordem, request);
        validarOrigemQuandoInventariada(ordem, request);

        ConteinerPatioRequisicaoDto requisicao = new ConteinerPatioRequisicaoDto();
        requisicao.setCodigo(ordem.getCodigoConteiner());
        requisicao.setLinha(ordem.getLinhaDestino());
        requisicao.setColuna(ordem.getColunaDestino());
        requisicao.setStatus(ordem.getStatusConteinerDestino());
        requisicao.setTipoCarga(ordem.getTipoCarga());
        requisicao.setDestino(ordem.getDestino());
        requisicao.setCamadaOperacional(ordem.getCamadaDestino());
        mapaPatioServico.registrarOuAtualizarConteiner(requisicao);
        conteinerRepositorio.findByCodigoIgnoreCase(ordem.getCodigoConteiner()).ifPresent(ordem::setConteiner);
        liberarReservaDestino(ordem);
    }

    private void confirmarUngrounding(OrdemTrabalhoPatio ordem,
                                       EventoVmtWorkInstructionRequest request) {
        String destino = obrigatorio(request.getDestino(), "O destino fisico");
        ConteinerPatio conteiner = conteinerRepositorio.findByCodigoIgnoreCase(ordem.getCodigoConteiner())
                .orElseThrow(() -> conflito("A unidade nao esta registrada no inventario do patio."));
        PosicaoPatio posicao = conteiner.getPosicao();
        if (posicao == null) {
            throw conflito("A unidade nao possui posicao fisica para ungrounding.");
        }
        validarOrigem(posicao, request);
        conteiner.setPosicao(null);
        conteiner.setDestino(destino.trim().toUpperCase(Locale.ROOT));
        conteiner.setStatus(ordem.getStatusConteinerDestino() == null
                ? StatusConteiner.DESPACHADO
                : ordem.getStatusConteinerDestino());
        conteinerRepositorio.save(conteiner);
        ordem.setConteiner(conteiner);
    }

    private void validarDestinoDaOrdem(OrdemTrabalhoPatio ordem,
                                       EventoVmtWorkInstructionRequest request) {
        String destino = obrigatorio(request.getDestino(), "O destino fisico");
        if (!destino.equalsIgnoreCase(ordem.getDestino())
                || !Objects.equals(request.getLinhaDestino(), ordem.getLinhaDestino())
                || !Objects.equals(request.getColunaDestino(), ordem.getColunaDestino())
                || !iguais(request.getCamadaDestino(), ordem.getCamadaDestino())) {
            throw conflito("O destino fisico informado diverge da work instruction.");
        }
    }

    private void validarOrigemQuandoInventariada(OrdemTrabalhoPatio ordem,
                                                  EventoVmtWorkInstructionRequest request) {
        ConteinerPatio conteiner = conteinerRepositorio.findByCodigoIgnoreCase(ordem.getCodigoConteiner()).orElse(null);
        if (conteiner == null || conteiner.getPosicao() == null) {
            return;
        }
        validarOrigem(conteiner.getPosicao(), request);
    }

    private void validarOrigem(PosicaoPatio posicao,
                               EventoVmtWorkInstructionRequest request) {
        obrigatorio(request.getOrigem(), "A origem fisica");
        if (!Objects.equals(request.getLinhaOrigem(), posicao.getLinha())
                || !Objects.equals(request.getColunaOrigem(), posicao.getColuna())
                || !iguais(request.getCamadaOrigem(), posicao.getCamadaOperacional())) {
            throw conflito("A origem fisica informada diverge da posicao atual do inventario.");
        }
    }

    private void liberarReservaDestino(OrdemTrabalhoPatio ordem) {
        if (!StringUtils.hasText(ordem.getChaveIdempotencia())) {
            return;
        }
        posicaoRepositorio.findByLinhaAndColunaAndCamadaOperacional(
                        ordem.getLinhaDestino(), ordem.getColunaDestino(), ordem.getCamadaDestino())
                .filter(posicao -> ordem.getChaveIdempotencia().equals(posicao.getReservaChave()))
                .ifPresent(posicao -> {
                    posicao.liberarReserva();
                    posicaoRepositorio.save(posicao);
                });
    }

    private TipoAcaoFisicaPatio tipoAcaoEsperado(TipoMovimentoPatio tipoMovimento) {
        return tipoMovimento == TipoMovimentoPatio.REMOCAO || tipoMovimento == TipoMovimentoPatio.LIBERACAO
                ? TipoAcaoFisicaPatio.UNGROUNDING
                : TipoAcaoFisicaPatio.GROUNDING;
    }

    private boolean iguais(String primeiro, String segundo) {
        return StringUtils.hasText(primeiro)
                && StringUtils.hasText(segundo)
                && primeiro.trim().equalsIgnoreCase(segundo.trim());
    }

    private String obrigatorio(String valor, String campo) {
        if (!StringUtils.hasText(valor)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " deve ser informado.");
        }
        return valor.trim();
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }
}
