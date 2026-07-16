package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AplicacaoPlanoOtimizadoPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AplicacaoPlanoOtimizadoPatioDto.ItemPlanoOtimizadoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.CompensacaoPlanoOtimizadoPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoAplicacaoPlanoOtimizadoPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoAplicacaoPlanoOtimizadoPatioDto.EstadoAnteriorOrdemDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoAplicacaoPlanoOtimizadoPatioDto.EstadoAnteriorWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.AplicacaoPlanoOtimizadoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.HistoricoOperacaoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusAplicacaoPlanoOtimizadoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusWorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.AplicacaoPlanoOtimizadoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.HistoricoWorkInstructionRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlanoOtimizadoPatioServico {

    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final WorkQueuePatioRepositorio workQueueRepositorio;
    private final PosicaoPatioRepositorio posicaoRepositorio;
    private final HistoricoWorkInstructionRepositorio historicoRepositorio;
    private final AplicacaoPlanoOtimizadoPatioRepositorio aplicacaoRepositorio;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public PlanoOtimizadoPatioServico(
            OrdemTrabalhoPatioRepositorio ordemRepositorio,
            WorkQueuePatioRepositorio workQueueRepositorio,
            PosicaoPatioRepositorio posicaoRepositorio,
            HistoricoWorkInstructionRepositorio historicoRepositorio,
            AplicacaoPlanoOtimizadoPatioRepositorio aplicacaoRepositorio,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.ordemRepositorio = ordemRepositorio;
        this.workQueueRepositorio = workQueueRepositorio;
        this.posicaoRepositorio = posicaoRepositorio;
        this.historicoRepositorio = historicoRepositorio;
        this.aplicacaoRepositorio = aplicacaoRepositorio;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ResultadoAplicacaoPlanoOtimizadoPatioDto aplicar(AplicacaoPlanoOtimizadoPatioDto comando) {
        validarComando(comando);
        try {
            ResultadoAplicacaoPlanoOtimizadoPatioDto resultado = transactionTemplate.execute(
                    status -> aplicarTransacional(comando));
            if (resultado == null) {
                throw new IllegalStateException("A transacao do plano otimizado nao retornou resultado.");
            }
            return resultado;
        } catch (DataIntegrityViolationException ex) {
            ResultadoAplicacaoPlanoOtimizadoPatioDto resultadoAnterior = transactionTemplate.execute(
                    status -> buscarResultadoConcluido(comando));
            if (resultadoAnterior != null) {
                return resultadoAnterior;
            }
            throw ex;
        }
    }

    private ResultadoAplicacaoPlanoOtimizadoPatioDto aplicarTransacional(
            AplicacaoPlanoOtimizadoPatioDto comando
    ) {
        AplicacaoPlanoOtimizadoPatio aplicacao = aplicacaoRepositorio
                .findByPlanoIdAndVisitaNavioId(comando.getPlanoId(), comando.getVisitaNavioId())
                .orElse(null);
        if (aplicacao != null
                && aplicacao.getStatus() == StatusAplicacaoPlanoOtimizadoPatio.CONCLUIDA) {
            return desserializarResultado(aplicacao);
        }
        if (aplicacao != null
                && aplicacao.getStatus() == StatusAplicacaoPlanoOtimizadoPatio.EM_ANDAMENTO) {
            throw conflito("O plano " + comando.getPlanoId() + " ja esta em aplicacao.");
        }
        if (aplicacao == null) {
            aplicacao = novaAplicacao(comando);
            aplicacaoRepositorio.saveAndFlush(aplicacao);
        } else {
            aplicacao.setStatus(StatusAplicacaoPlanoOtimizadoPatio.EM_ANDAMENTO);
            aplicacao.setResultadoJson(null);
            aplicacao.setAtualizadoEm(LocalDateTime.now());
            aplicacaoRepositorio.save(aplicacao);
        }

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
        resultado.setEstadosAnterioresWorkQueues(estadosAnterioresFilas(alteracoes));

        Map<Long, Integer> menorPrioridadePorFila = new HashMap<>();
        LocalDateTime agora = LocalDateTime.now();
        for (AlteracaoPlanejada alteracao : alteracoes) {
            OrdemTrabalhoPatio ordem = alteracao.ordem();
            PosicaoPatio posicao = alteracao.posicao();
            ItemPlanoOtimizadoDto itemPlano = alteracao.itemPlano();
            WorkQueuePatio fila = alteracao.fila();

            ordem.setDestino(posicao.getBloco().trim().toUpperCase(Locale.ROOT));
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
                    Integer::min);
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
        aplicacao.setStatus(StatusAplicacaoPlanoOtimizadoPatio.CONCLUIDA);
        aplicacao.setResultadoJson(serializarResultado(resultado));
        aplicacao.setAtualizadoEm(agora);
        aplicacaoRepositorio.save(aplicacao);
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
        AplicacaoPlanoOtimizadoPatio aplicacao = aplicacaoRepositorio
                .findByPlanoIdAndVisitaNavioId(comando.getPlanoId(), comando.getVisitaNavioId())
                .orElseThrow(() -> conflito("A aplicacao do plano informado nao foi encontrada."));
        if (aplicacao.getStatus() == StatusAplicacaoPlanoOtimizadoPatio.COMPENSADA) {
            return;
        }
        if (aplicacao.getStatus() != StatusAplicacaoPlanoOtimizadoPatio.CONCLUIDA) {
            throw conflito("O plano " + comando.getPlanoId()
                    + " nao esta concluido e nao pode ser compensado.");
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

        for (EstadoAnteriorWorkQueueDto estadoFila : comando.getEstadosAnterioresWorkQueues()) {
            WorkQueuePatio fila = workQueueRepositorio.findById(estadoFila.getWorkQueueId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Work queue da compensacao nao encontrada."));
            if (!Objects.equals(comando.getVisitaNavioId(), fila.getVisitaNavioId())) {
                throw conflito("A work queue " + fila.getId() + " nao pertence a visita da compensacao.");
            }
            fila.setSequenciaInicial(estadoFila.getSequenciaInicial());
            fila.setPrioridadeOperacional(estadoFila.getPrioridadeOperacional());
            fila.setAtualizadoEm(LocalDateTime.now());
            workQueueRepositorio.save(fila);
            registrarHistorico(
                    fila.getId(),
                    null,
                    "WORK_QUEUE_PLANO_OTIMIZADO_COMPENSADA",
                    comando.getUsuario(),
                    "planoId=" + comando.getPlanoId() + "; motivo=" + comando.getMotivo());
        }
        aplicacao.setStatus(StatusAplicacaoPlanoOtimizadoPatio.COMPENSADA);
        aplicacao.setAtualizadoEm(LocalDateTime.now());
        aplicacaoRepositorio.save(aplicacao);
    }

    private AplicacaoPlanoOtimizadoPatio novaAplicacao(AplicacaoPlanoOtimizadoPatioDto comando) {
        LocalDateTime agora = LocalDateTime.now();
        AplicacaoPlanoOtimizadoPatio aplicacao = new AplicacaoPlanoOtimizadoPatio();
        aplicacao.setPlanoId(comando.getPlanoId().trim());
        aplicacao.setVisitaNavioId(comando.getVisitaNavioId());
        aplicacao.setStatus(StatusAplicacaoPlanoOtimizadoPatio.EM_ANDAMENTO);
        aplicacao.setCriadoEm(agora);
        aplicacao.setAtualizadoEm(agora);
        return aplicacao;
    }

    private ResultadoAplicacaoPlanoOtimizadoPatioDto buscarResultadoConcluido(
            AplicacaoPlanoOtimizadoPatioDto comando
    ) {
        return aplicacaoRepositorio
                .findByPlanoIdAndVisitaNavioId(comando.getPlanoId(), comando.getVisitaNavioId())
                .filter(aplicacao -> aplicacao.getStatus() == StatusAplicacaoPlanoOtimizadoPatio.CONCLUIDA)
                .map(this::desserializarResultado)
                .orElse(null);
    }

    private String serializarResultado(ResultadoAplicacaoPlanoOtimizadoPatioDto resultado) {
        try {
            return objectMapper.writeValueAsString(resultado);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Nao foi possivel persistir o resultado do plano otimizado.", ex);
        }
    }

    private ResultadoAplicacaoPlanoOtimizadoPatioDto desserializarResultado(
            AplicacaoPlanoOtimizadoPatio aplicacao
    ) {
        if (!StringUtils.hasText(aplicacao.getResultadoJson())) {
            throw conflito("A aplicacao concluida do plano nao possui resultado persistido.");
        }
        try {
            return objectMapper.readValue(
                    aplicacao.getResultadoJson(),
                    ResultadoAplicacaoPlanoOtimizadoPatioDto.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Nao foi possivel recuperar o resultado do plano otimizado.", ex);
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
        comando.setPlanoId(comando.getPlanoId().trim());
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
        if (!StringUtils.hasText(posicao.getBloco())) {
            throw conflito("A posicao " + chavePosicao
                    + " nao possui bloco operacional para validar a work queue.");
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
        return candidatas.stream()
                .filter(fila -> StringUtils.hasText(fila.getBlocoZona()))
                .filter(fila -> fila.getBlocoZona().equalsIgnoreCase(bloco))
                .findFirst()
                .orElseThrow(() -> conflito("Nao existe work queue compativel com o equipamento "
                        + equipamento + " e o bloco de destino " + bloco + "."));
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

    private List<EstadoAnteriorWorkQueueDto> estadosAnterioresFilas(
            List<AlteracaoPlanejada> alteracoes
    ) {
        Map<Long, WorkQueuePatio> filas = new LinkedHashMap<>();
        alteracoes.forEach(alteracao -> filas.putIfAbsent(
                alteracao.fila().getId(),
                alteracao.fila()));
        return filas.values().stream().map(fila -> {
            EstadoAnteriorWorkQueueDto estado = new EstadoAnteriorWorkQueueDto();
            estado.setWorkQueueId(fila.getId());
            estado.setSequenciaInicial(fila.getSequenciaInicial());
            estado.setPrioridadeOperacional(fila.getPrioridadeOperacional());
            return estado;
        }).toList();
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
        if (linha == null || coluna == null || !StringUtils.hasText(camada)) {
            throw conflito("A posicao do plano deve informar linha, coluna e camada.");
        }
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
