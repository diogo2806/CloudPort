package br.com.cloudport.servicoyard.scheduler.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.scheduler.dto.AlteracaoEstadoPlanoPosicaoDto;
import br.com.cloudport.servicoyard.scheduler.dto.HistoricoPlanoPosicaoOperacionalDto;
import br.com.cloudport.servicoyard.scheduler.dto.PlanoPosicaoOperacionalRespostaDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerAssignmentDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerPlanoOperacionalRequisicaoDto;
import br.com.cloudport.servicoyard.scheduler.modelo.EstadoPlanoPosicaoOperacional;
import br.com.cloudport.servicoyard.scheduler.modelo.HistoricoPlanoPosicaoOperacional;
import br.com.cloudport.servicoyard.scheduler.modelo.PlanoPosicaoOperacional;
import br.com.cloudport.servicoyard.scheduler.repositorio.HistoricoPlanoPosicaoOperacionalRepositorio;
import br.com.cloudport.servicoyard.scheduler.repositorio.PlanoPosicaoOperacionalRepositorio;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlanoPosicaoOperacionalServico {

    private static final Set<EstadoPlanoPosicaoOperacional> ESTADOS_ATIVOS = EnumSet.of(
            EstadoPlanoPosicaoOperacional.TENTATIVO,
            EstadoPlanoPosicaoOperacional.DEFINITIVO,
            EstadoPlanoPosicaoOperacional.IMINENTE);

    private final PlanoPosicaoOperacionalRepositorio repositorio;
    private final HistoricoPlanoPosicaoOperacionalRepositorio historicoRepositorio;

    public PlanoPosicaoOperacionalServico(
            PlanoPosicaoOperacionalRepositorio repositorio,
            HistoricoPlanoPosicaoOperacionalRepositorio historicoRepositorio) {
        this.repositorio = repositorio;
        this.historicoRepositorio = historicoRepositorio;
    }

    @Transactional
    public List<PlanoPosicaoOperacionalRespostaDto> registrarPropostas(
            SchedulerPlanoOperacionalRequisicaoDto requisicao,
            List<SchedulerAssignmentDto> atribuicoes,
            String assinaturaEntrada) {
        if (requisicao == null || requisicao.getNavio() == null || atribuicoes == null || atribuicoes.isEmpty()) {
            return List.of();
        }
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime horizonteInicio = requisicao.getNavio().getEtaChegada().isAfter(agora)
                ? agora
                : requisicao.getNavio().getEtaChegada();
        LocalDateTime horizonteFim = requisicao.getNavio().getEtaPartida();
        if (!horizonteFim.isAfter(horizonteInicio)) {
            horizonteFim = horizonteInicio.plusHours(6);
        }
        LocalDateTime validade = definirValidade(requisicao, agora, horizonteFim);
        String assinatura = StringUtils.hasText(assinaturaEntrada)
                ? assinaturaEntrada.trim()
                : "SEM_ASSINATURA_" + agora;

        for (SchedulerAssignmentDto atribuicao : atribuicoes) {
            if (!StringUtils.hasText(atribuicao.getCodigoContainer())
                    || atribuicao.getLinhaProposta() == null
                    || atribuicao.getColunaProposta() == null
                    || !StringUtils.hasText(atribuicao.getCamadaProposta())) {
                continue;
            }
            if (repositorio.findByAssinaturaEntradaAndCodigoContainerIgnoreCase(
                    assinatura,
                    atribuicao.getCodigoContainer()).isPresent()) {
                continue;
            }
            PlanoPosicaoOperacional plano = new PlanoPosicaoOperacional();
            plano.setCodigoContainer(normalizar(atribuicao.getCodigoContainer()));
            plano.setBloco(normalizarOpcional(atribuicao.getBlocoProposto()));
            plano.setLinha(atribuicao.getLinhaProposta());
            plano.setColuna(atribuicao.getColunaProposta());
            plano.setCamada(normalizar(atribuicao.getCamadaProposta()));
            plano.setEquipamentoId(normalizarOpcional(atribuicao.getEquipamentoId()));
            plano.setEstado(EstadoPlanoPosicaoOperacional.TENTATIVO);
            plano.setHorizonteInicio(horizonteInicio);
            plano.setHorizonteFim(horizonteFim);
            plano.setValidoAte(validade);
            plano.setOrigem("SCHEDULER_PREDITIVO");
            plano.setMotivo("Proposta calculada pelo otimizador real e pendente de confirmação operacional.");
            plano.setAssinaturaEntrada(assinatura);
            plano.setAlteradoPor("SCHEDULER");
            PlanoPosicaoOperacional salvo = repositorio.saveAndFlush(plano);
            registrarHistorico(salvo, null, EstadoPlanoPosicaoOperacional.TENTATIVO,
                    salvo.getMotivo(), "SCHEDULER");
        }
        return listar(null, null);
    }

    @Transactional
    public List<PlanoPosicaoOperacionalRespostaDto> listar(
            EstadoPlanoPosicaoOperacional estado,
            String bloco) {
        expirarPlanosVencidos();
        return repositorio.findAllByOrderByHorizonteInicioAscCodigoContainerAsc().stream()
                .filter(plano -> estado == null || plano.getEstado() == estado)
                .filter(plano -> !StringUtils.hasText(bloco)
                        || (plano.getBloco() != null && plano.getBloco().equalsIgnoreCase(bloco.trim())))
                .map(PlanoPosicaoOperacionalRespostaDto::deEntidade)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoPlanoPosicaoOperacionalDto> listarHistorico(Long planoId) {
        return historicoRepositorio.findByPlanoIdOrderByOcorridoEmDesc(planoId).stream()
                .map(HistoricoPlanoPosicaoOperacionalDto::deEntidade)
                .toList();
    }

    @Transactional
    public PlanoPosicaoOperacionalRespostaDto alterarEstado(
            Long planoId,
            AlteracaoEstadoPlanoPosicaoDto comando) {
        PlanoPosicaoOperacional plano = buscarParaAtualizacao(planoId);
        EstadoPlanoPosicaoOperacional destino = comando.getEstadoDestino();
        validarTransicao(plano, destino);
        aplicarTransicao(plano, destino, comando.getMotivo(), comando.getOperador());
        return PlanoPosicaoOperacionalRespostaDto.deEntidade(plano);
    }

    @Transactional
    public void revalidarParaDispatch(OrdemTrabalhoPatio ordem, String operador) {
        if (ordem == null || !StringUtils.hasText(ordem.getCodigoConteiner())) {
            return;
        }
        PlanoPosicaoOperacional plano = repositorio
                .findFirstByCodigoContainerIgnoreCaseAndEstadoInOrderByAtualizadoEmDesc(
                        ordem.getCodigoConteiner(), ESTADOS_ATIVOS)
                .orElse(null);
        if (plano == null) {
            return;
        }
        LocalDateTime agora = LocalDateTime.now();
        if (plano.expiradoEm(agora)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A posição planejada para a unidade " + ordem.getCodigoConteiner()
                            + " expirou e deve ser recalculada antes do dispatch.");
        }
        if (!plano.correspondeAoDestino(
                ordem.getLinhaDestino(), ordem.getColunaDestino(), ordem.getCamadaDestino())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O destino da work instruction diverge da posição do planejamento preditivo.");
        }
        if (plano.getEstado() == EstadoPlanoPosicaoOperacional.TENTATIVO) {
            aplicarTransicao(
                    plano,
                    EstadoPlanoPosicaoOperacional.DEFINITIVO,
                    "Posição tentativa revalidada transacionalmente durante o dispatch.",
                    StringUtils.hasText(operador) ? operador : "DISPATCH");
        }
        if (!plano.getEstado().permiteDispatchDireto()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O estado " + plano.getEstado() + " não permite dispatch da posição planejada.");
        }
        plano.setOrdemTrabalhoPatioId(ordem.getId());
        plano.setAlteradoPor(StringUtils.hasText(operador) ? operador.trim() : "DISPATCH");
        repositorio.save(plano);
    }

    private PlanoPosicaoOperacional buscarParaAtualizacao(Long planoId) {
        return repositorio.findByIdForUpdate(planoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Plano de posição operacional não encontrado."));
    }

    private void validarTransicao(
            PlanoPosicaoOperacional plano,
            EstadoPlanoPosicaoOperacional destino) {
        if (destino == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O estado de destino deve ser informado.");
        }
        if (plano.getEstado() == destino) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O plano já está no estado informado.");
        }
        if (plano.expiradoEm(LocalDateTime.now()) && destino != EstadoPlanoPosicaoOperacional.EXPIRADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O plano expirou e não pode ser convertido sem novo cálculo.");
        }
        boolean permitida = switch (plano.getEstado()) {
            case TENTATIVO -> destino == EstadoPlanoPosicaoOperacional.DEFINITIVO
                    || destino == EstadoPlanoPosicaoOperacional.CANCELADO
                    || destino == EstadoPlanoPosicaoOperacional.EXPIRADO;
            case DEFINITIVO -> destino == EstadoPlanoPosicaoOperacional.IMINENTE
                    || destino == EstadoPlanoPosicaoOperacional.CANCELADO
                    || destino == EstadoPlanoPosicaoOperacional.EXPIRADO;
            case IMINENTE -> destino == EstadoPlanoPosicaoOperacional.CANCELADO
                    || destino == EstadoPlanoPosicaoOperacional.EXPIRADO;
            case EXPIRADO, CANCELADO -> false;
        };
        if (!permitida) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transição de " + plano.getEstado() + " para " + destino + " não permitida.");
        }
    }

    private void aplicarTransicao(
            PlanoPosicaoOperacional plano,
            EstadoPlanoPosicaoOperacional destino,
            String motivo,
            String operador) {
        EstadoPlanoPosicaoOperacional anterior = plano.getEstado();
        plano.setEstado(destino);
        plano.setMotivo(limitar(motivo, 1000));
        plano.setAlteradoPor(limitar(operador, 120));
        plano.setConvertidoEm(LocalDateTime.now());
        PlanoPosicaoOperacional salvo = repositorio.saveAndFlush(plano);
        registrarHistorico(salvo, anterior, destino, motivo, operador);
    }

    private void expirarPlanosVencidos() {
        LocalDateTime agora = LocalDateTime.now();
        List<PlanoPosicaoOperacional> vencidos = repositorio
                .findByEstadoInAndHorizonteFimAfterOrderByHorizonteInicioAsc(ESTADOS_ATIVOS, agora.minusYears(100))
                .stream()
                .filter(plano -> plano.expiradoEm(agora))
                .toList();
        for (PlanoPosicaoOperacional plano : vencidos) {
            EstadoPlanoPosicaoOperacional anterior = plano.getEstado();
            plano.setEstado(EstadoPlanoPosicaoOperacional.EXPIRADO);
            plano.setMotivo("Validade operacional encerrada sem confirmação ou execução.");
            plano.setAlteradoPor("SISTEMA");
            plano.setConvertidoEm(agora);
            PlanoPosicaoOperacional salvo = repositorio.saveAndFlush(plano);
            registrarHistorico(salvo, anterior, EstadoPlanoPosicaoOperacional.EXPIRADO,
                    salvo.getMotivo(), "SISTEMA");
        }
    }

    private void registrarHistorico(
            PlanoPosicaoOperacional plano,
            EstadoPlanoPosicaoOperacional anterior,
            EstadoPlanoPosicaoOperacional novo,
            String motivo,
            String operador) {
        HistoricoPlanoPosicaoOperacional historico = new HistoricoPlanoPosicaoOperacional();
        historico.setPlanoId(plano.getId());
        historico.setEstadoAnterior(anterior);
        historico.setEstadoNovo(novo);
        historico.setMotivo(limitar(motivo, 1000));
        historico.setOperador(limitar(operador, 120));
        historico.setVersaoPlano(plano.getVersao() == null ? 0L : plano.getVersao());
        historicoRepositorio.save(historico);
    }

    private LocalDateTime definirValidade(
            SchedulerPlanoOperacionalRequisicaoDto requisicao,
            LocalDateTime agora,
            LocalDateTime horizonteFim) {
        LocalDateTime cutoff = requisicao.getCutoffOperacional();
        if (cutoff != null && cutoff.isAfter(agora)) {
            return cutoff.isBefore(horizonteFim) ? cutoff : horizonteFim;
        }
        LocalDateTime padrao = agora.plusHours(2);
        return padrao.isBefore(horizonteFim) ? padrao : horizonteFim;
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarOpcional(String valor) {
        return StringUtils.hasText(valor) ? normalizar(valor) : null;
    }

    private String limitar(String valor, int limite) {
        String limpo = StringUtils.hasText(valor) ? valor.trim() : "Não informado";
        return limpo.length() <= limite ? limpo : limpo.substring(0, limite);
    }
}
