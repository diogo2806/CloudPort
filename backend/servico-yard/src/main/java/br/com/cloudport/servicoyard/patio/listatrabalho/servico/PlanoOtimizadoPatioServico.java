package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AplicacaoPlanoOtimizadoPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AplicacaoPlanoOtimizadoPatioDto.ItemPlanoOtimizadoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.CompensacaoPlanoOtimizadoPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoAplicacaoPlanoOtimizadoPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoAplicacaoPlanoOtimizadoPatioDto.EstadoAnteriorOrdemDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.HistoricoOperacaoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusWorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.HistoricoWorkInstructionRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlanoOtimizadoPatioServico {

    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final WorkQueuePatioRepositorio workQueueRepositorio;
    private final PosicaoPatioRepositorio posicaoRepositorio;
    private final HistoricoWorkInstructionRepositorio historicoRepositorio;

    public PlanoOtimizadoPatioServico(
            OrdemTrabalhoPatioRepositorio ordemRepositorio,
            WorkQueuePatioRepositorio workQueueRepositorio,
            PosicaoPatioRepositorio posicaoRepositorio,
            HistoricoWorkInstructionRepositorio historicoRepositorio
    ) {
        this.ordemRepositorio = ordemRepositorio;
        this.workQueueRepositorio = workQueueRepositorio;
        this.posicaoRepositorio = posicaoRepositorio;
        this.historicoRepositorio = historicoRepositorio;
    }

    @Transactional
    public ResultadoAplicacaoPlanoOtimizadoPatioDto aplicar(AplicacaoPlanoOtimizadoPatioDto comando) {
        validarComando(comando);
        List<WorkQueuePatio> filas = workQueueRepositorio
                .findByVisitaNavioIdOrderBySequenciaInicialAscCriadoEmAsc(comando.getVisitaNavioId());
        if (filas.isEmpty()) {
            throw conflito("A visita nao possui work queues reais para aplicar o plano otimizado.");
        }

        Set<Long> ordensInformadas = new HashSet<>();
        Set<Long> itensInformados = new HashSet<>();
        Set<String> posicoesInformadas = new HashSet<>();
        List<AlteracaoPlanejada> alteracoes = new ArrayList<>();

        for (ItemPlanoOtimizadoDto itemPlano : comando.getItens()) {
            if (!ordensInformadas.add(itemPlano.getOrdemTrabalhoPatioId())) {
                throw conflito("O plano repete a ordem " + itemPlano.getOrdemTrabalhoPatioId() + ".");
            }
            if (!itensInformados.add(itemPlano.getItemOperacaoNavioId())) {
                throw conflito("O plano repete o item de navio " + itemPlano.getItemOperacaoNavioId() + ".");
            }
            String chavePosicao = chavePosicao(itemPlano.getLinha(), itemPlano.getColuna(), itemPlano.getCamada());
            if (!posicoesInformadas.add(chavePosicao)) {
                throw conflito("O plano atribui mais de uma carga para a posicao " + chavePosicao + ".");
            }

            OrdemTrabalhoPatio ordem = ordemRepositorio.findById(itemPlano.getOrdemTrabalhoPatioId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Ordem de trabalho do plano nao encontrada."));
            validarOrdem(comando, itemPlano, ordem);

            PosicaoPatio posicao = posicaoRepositorio
                    .findByLinhaAndColunaAndCamadaOperacional(
                            itemPlano.getLinha(),
                            itemPlano.getColuna(),
                            itemPlano.getCamada())
                    .orElseThrow(() -> conflito(
                            "A posicao " + chavePosicao + " nao existe no mapa real do patio."));
            validarPosicao(posicao, chavePosicao);
            WorkQueuePatio fila = selecionarFila(filas, itemPlano.getEquipamento(), posicao.getBloco());
            alteracoes.add(new AlteracaoPlanejada(ordem, posicao, fila, itemPlano));
        }

        ResultadoAplicacaoPlanoOtimizadoPatioDto resultado = new ResultadoAplicacaoPlanoOtimizadoPatioDto();
        resultado.setPlanoId(comando.getPlanoId());
        resultado.setVisitaNavioId(comando.getVisitaNavioId());
        resultado.setEstadosAnteriores(alteracoes.stream()
                .map(alteracao -> estadoAnterior(alteracao.ordem()))
                .toList());

        Map<Long, Integer> menorPrioridadePorFila = new HashMap<>();
        LocalDateTime agora = LocalDateTime.now();
        for (AlteracaoPlanejada alteracao : alteracoes) {
            OrdemTrabalhoPatio ordem = alteracao.ordem();
            PosicaoPatio posicao = alteracao.posicao();
            ItemPlanoOtimizadoDto itemPlano = alteracao.itemPlano();
            WorkQueuePatio fila = alteracao.fila();

            ordem.setDestino(StringUtils.hasText(posicao.getBloco())
                    ? posicao.getBloco().trim().toUpperCase(Locale.ROOT)
                    : chavePosicao(itemPlano.getLinha(), itemPlano.getColuna(), itemPlano.getCamada()));
            ordem.setLinhaDestino(itemPlano.getLinha());
            ordem.setColunaDestino(itemPlano.getColuna());
            ordem.setCamadaDestino(itemPlano.getCamada());
            ordem.setPrioridadeOperacional(itemPlano.getPrioridadeOperacional());
            ordem.setSequenciaNavio(itemPlano.getPrioridadeOperacional());
            ordem.setWorkQueueId(fila.getId());
            ordem.setAtualizadoEm(agora);
            ordemRepositorio.save(ordem);

            menorPrioridadePorFila.merge(
                    fila.getId(),
                    itemPlano.getPrioridadeOperacional(),
                    Math::min);
            registrarHistorico(
                    fila.getId(),
                    ordem.getId(),
                    "PLANO_OTIMIZADO_APLICADO",
                    comando.getUsuario(),
                    "planoId=" + comando.getPlanoId()
                            + "; equipamento=" + itemPlano.getEquipamento()
                            + "; posicao=" + chavePosicao(
                                    itemPlano.getLinha(), itemPlano.getColuna(), itemPlano.getCamada())
                            + "; prioridade=" + itemPlano.getPrioridadeOperacional());
        }

        for (WorkQueuePatio fila : filas) {
            Integer menorPrioridade = menorPrioridadePorFila.get(fila.getId());
            if (menorPrioridade != null) {
                fila.setSequenciaInicial(menorPrioridade);
                fila.setPrioridadeOperacional(menorPrioridade);
                fila.setAtualizadoEm(agora);
                workQueueRepositorio.save(fila);
            }
        }
        resultado.setOrdensAtualizadas(alteracoes.size());
        return resultado;
    }

    @Transactional
    public void compensar(CompensacaoPlanoOtimizadoPatioDto comando) {
        if (comando == null || !StringUtils.hasText(comando.getPlanoId())
                || comando.getVisitaNavioId() == null
                || comando.getEstadosAnteriores() == null
                || comando.getEstadosAnteriores().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dados de compensacao do plano otimizado incompletos.");
        }

        for (EstadoAnteriorOrdemDto estado : comando.getEstadosAnteriores()) {
            OrdemTrabalhoPatio ordem = ordemRepositorio.findById(estado.getOrdemTrabalhoPatioId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Ordem de trabalho da compensacao nao encontrada."));
            if (!Objects.equals(comando.getVisitaNavioId(), ordem.getVisitaNavioId())) {
                throw conflito("A ordem " + ordem.getId() + " nao pertence a visita da compensacao.");
            }
            ordem.setDestino(estado.getDestino());
            ordem.setLinhaDestino(estado.getLinhaDestino());
            ordem.setColunaDestino(estado.getColunaDestino());
            ordem.setCamadaDestino(estado.getCamadaDestino());
            ordem.setPrioridadeOperacional(estado.getPrioridadeOperacional());
            ordem.setSequenciaNavio(estado.getSequenciaNavio());
            ordem.setWorkQueueId(estado.getWorkQueueId());
            ordem.setAtualizadoEm(LocalDateTime.now());
            ordemRepositorio.save(ordem);
            registrarHistorico(
                    estado.getWorkQueueId(),
                    ordem.getId(),
                    "PLANO_OTIMIZADO_COMPENSADO",
                    comando.getUsuario(),
                    "planoId=" + comando.getPlanoId() + "; motivo=" + comando.getMotivo());
        }
    }

    private void validarComando(AplicacaoPlanoOtimizadoPatioDto comando) {
        if (comando == null || !StringUtils.hasText(comando.getPlanoId())
                || comando.getVisitaNavioId() == null
                || comando.getItens() == null
                || comando.getItens().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Plano otimizado, visita e itens devem ser informados.");
        }
    }

    private void validarOrdem(
            AplicacaoPlanoOtimizadoPatioDto comando,
            ItemPlanoOtimizadoDto itemPlano,
            OrdemTrabalhoPatio ordem
    ) {
        if (!Objects.equals(comando.getVisitaNavioId(), ordem.getVisitaNavioId())) {
            throw conflito("A ordem " + ordem.getId() + " pertence a outra visita.");
        }
        if (!Objects.equals(itemPlano.getItemOperacaoNavioId(), ordem.getItemOperacaoNavioId())) {
            throw conflito("A ordem " + ordem.getId() + " nao pertence ao item informado no plano.");
        }
        if (!ordem.getCodigoConteiner().equalsIgnoreCase(itemPlano.getCodigoConteiner())) {
            throw conflito("O codigo da carga da ordem " + ordem.getId() + " diverge do plano.");
        }
        if (ordem.getStatusOrdem() != StatusOrdemTrabalhoPatio.PENDENTE
                && ordem.getStatusOrdem() != StatusOrdemTrabalhoPatio.SUSPENSA) {
            throw conflito("A ordem " + ordem.getId() + " nao pode ser replanejada no status "
                    + ordem.getStatusOrdem() + ".");
        }
    }

    private void validarPosicao(PosicaoPatio posicao, String chavePosicao) {
        if (posicao.isBloqueada()) {
            throw conflito("A posicao " + chavePosicao + " esta bloqueada.");
        }
        if (posicao.isInterditada()) {
            throw conflito("A posicao " + chavePosicao + " esta interditada.");
        }
        if (!posicao.isAreaPermitida()) {
            throw conflito("A posicao " + chavePosicao + " esta fora da area operacional permitida.");
        }
    }

    private WorkQueuePatio selecionarFila(
            List<WorkQueuePatio> filas,
            String equipamento,
            String bloco
    ) {
        List<WorkQueuePatio> candidatas = filas.stream()
                .filter(fila -> fila.getStatus() == StatusWorkQueuePatio.ATIVA)
                .filter(fila -> fila.getEquipamentoPatioId() != null)
                .filter(fila -> StringUtils.hasText(fila.getEquipamento()))
                .filter(fila -> fila.getEquipamento().equalsIgnoreCase(equipamento))
                .filter(fila -> StringUtils.hasText(fila.getPow()))
                .filter(fila -> StringUtils.hasText(fila.getPoolOperacional()))
                .toList();
        if (candidatas.isEmpty()) {
            throw conflito("Nao existe work queue ativa, coberta e com recursos reais para o equipamento "
                    + equipamento + ".");
        }
        if (StringUtils.hasText(bloco)) {
            return candidatas.stream()
                    .filter(fila -> StringUtils.hasText(fila.getBlocoZona()))
                    .filter(fila -> fila.getBlocoZona().equalsIgnoreCase(bloco))
                    .findFirst()
                    .orElse(candidatas.get(0));
        }
        return candidatas.get(0);
    }

    private EstadoAnteriorOrdemDto estadoAnterior(OrdemTrabalhoPatio ordem) {
        EstadoAnteriorOrdemDto estado = new EstadoAnteriorOrdemDto();
        estado.setOrdemTrabalhoPatioId(ordem.getId());
        estado.setDestino(ordem.getDestino());
        estado.setLinhaDestino(ordem.getLinhaDestino());
        estado.setColunaDestino(ordem.getColunaDestino());
        estado.setCamadaDestino(ordem.getCamadaDestino());
        estado.setPrioridadeOperacional(ordem.getPrioridadeOperacional());
        estado.setSequenciaNavio(ordem.getSequenciaNavio());
        estado.setWorkQueueId(ordem.getWorkQueueId());
        return estado;
    }

    private void registrarHistorico(
            Long workQueueId,
            Long ordemId,
            String acao,
            String usuario,
            String detalhes
    ) {
        HistoricoOperacaoPatio historico = new HistoricoOperacaoPatio();
        historico.setWorkQueueId(workQueueId);
        historico.setOrdemTrabalhoPatioId(ordemId);
        historico.setAcao(acao);
        historico.setUsuario(StringUtils.hasText(usuario) ? usuario.trim() : "sistema");
        historico.setMotivo("Aplicacao transacional do resultado real do otimizador.");
        historico.setDetalhes(detalhes.length() <= 2000 ? detalhes : detalhes.substring(0, 2000));
        historico.setCriadoEm(LocalDateTime.now());
        historicoRepositorio.save(historico);
    }

    private String chavePosicao(Integer linha, Integer coluna, String camada) {
        return linha + "-" + coluna + "-" + camada.trim().toUpperCase(Locale.ROOT);
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }

    private record AlteracaoPlanejada(
            OrdemTrabalhoPatio ordem,
            PosicaoPatio posicao,
            WorkQueuePatio fila,
            ItemPlanoOtimizadoDto itemPlano
    ) {
    }
}
