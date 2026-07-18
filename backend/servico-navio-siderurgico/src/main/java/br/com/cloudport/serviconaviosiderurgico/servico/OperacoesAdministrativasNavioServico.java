package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.contracts.api.ComandoMotivado;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.OrdemPatioYardRespostaDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardComandoCliente;
import br.com.cloudport.serviconaviosiderurgico.dominio.FaseVisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.PlanoEstivaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.PosicaoEstivaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusPlanoEstivaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.ItemOperacaoNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.OrdemPatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PlanoEstivaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PosicaoEstivaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.VisitaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.PlanoEstivaNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.PosicaoEstivaNavioRepositorio;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OperacoesAdministrativasNavioServico {

    private static final Set<String> STATUS_ORDEM_TERMINAIS = Set.of("CONCLUIDA", "CANCELADA");

    private final VisitaNavioServico visitaServico;
    private final ItemOperacaoNavioServico itemServico;
    private final PlanoEstivaNavioServico planoServico;
    private final PlanoEstivaNavioRepositorio planoRepositorio;
    private final PosicaoEstivaNavioRepositorio posicaoRepositorio;
    private final OrdemPatioYardCliente ordemConsultaCliente;
    private final OrdemPatioYardComandoCliente ordemComandoCliente;
    private final SincronizadorStatusNavioPatioServico sincronizadorStatus;

    public OperacoesAdministrativasNavioServico(
            VisitaNavioServico visitaServico,
            ItemOperacaoNavioServico itemServico,
            PlanoEstivaNavioServico planoServico,
            PlanoEstivaNavioRepositorio planoRepositorio,
            PosicaoEstivaNavioRepositorio posicaoRepositorio,
            OrdemPatioYardCliente ordemConsultaCliente,
            OrdemPatioYardComandoCliente ordemComandoCliente,
            SincronizadorStatusNavioPatioServico sincronizadorStatus) {
        this.visitaServico = visitaServico;
        this.itemServico = itemServico;
        this.planoServico = planoServico;
        this.planoRepositorio = planoRepositorio;
        this.posicaoRepositorio = posicaoRepositorio;
        this.ordemConsultaCliente = ordemConsultaCliente;
        this.ordemComandoCliente = ordemComandoCliente;
        this.sincronizadorStatus = sincronizadorStatus;
    }

    @Transactional
    public VisitaNavioDTO cancelarVisita(Long visitaId, ComandoMotivado comando) {
        VisitaNavio visita = visitaServico.buscarEntidade(visitaId);
        if (visita.getFase() == FaseVisitaNavio.CANCELADA) {
            return VisitaNavioDTO.de(visita);
        }
        if (visita.getFase() == FaseVisitaNavio.PARTIU) {
            throw new IllegalArgumentException("Nao e permitido cancelar administrativamente uma visita que ja partiu.");
        }

        String motivo = motivo(comando);
        String usuario = usuario(comando);
        List<ItemOperacaoNavioDTO> itens = itemServico.listar(visitaId, null, null);
        for (ItemOperacaoNavioDTO item : itens) {
            if (item.status() != StatusItemCarga.CANCELADO && item.status() != StatusItemCarga.OPERADO) {
                itemServico.alterarStatus(
                        visitaId,
                        item.id(),
                        StatusItemCarga.CANCELADO,
                        usuario,
                        "Cancelamento administrativo da visita: " + motivo);
            }
        }

        cancelarOrdensDaVisitaComCompensacao(visita, comando);
        return visitaServico.alterarFase(
                visitaId,
                FaseVisitaNavio.CANCELADA,
                usuario,
                "Cancelamento administrativo da visita: " + motivo);
    }

    @Transactional
    public ItemOperacaoNavioDTO cancelarItem(Long visitaId, Long itemId, ComandoMotivado comando) {
        ItemOperacaoNavio item = itemServico.buscarEntidade(visitaId, itemId);
        if (item.getStatus() == StatusItemCarga.CANCELADO) {
            return ItemOperacaoNavioDTO.de(item);
        }
        if (item.getStatus() == StatusItemCarga.OPERADO) {
            throw new IllegalArgumentException("Nao e permitido cancelar item que ja foi operado.");
        }

        cancelarOrdensDoItemComCompensacao(item, comando);
        return itemServico.alterarStatus(
                visitaId,
                itemId,
                StatusItemCarga.CANCELADO,
                usuario(comando),
                "Cancelamento administrativo do item: " + motivo(comando));
    }

    @Transactional
    public PlanoEstivaNavioDTO publicarPlano(Long visitaId, Long planoId, ComandoMotivado comando) {
        PlanoEstivaNavio plano = buscarPlano(visitaId, planoId);
        if (plano.getStatus() == StatusPlanoEstivaNavio.CONCLUIDO) {
            return dto(plano);
        }
        if (plano.getStatus() != StatusPlanoEstivaNavio.VALIDADO) {
            throw new IllegalArgumentException("Somente plano validado pode ser concluido e publicado.");
        }

        PlanoEstivaNavioDTO concluido = planoServico.concluir(visitaId, planoId);
        visitaServico.registrarEvento(
                plano.getVisitaNavio(),
                null,
                "PLANO_PUBLICADO",
                "Plano de estiva versao " + plano.getVersao() + " publicado. Motivo: " + motivo(comando),
                usuario(comando),
                StatusPlanoEstivaNavio.VALIDADO.name(),
                StatusPlanoEstivaNavio.CONCLUIDO.name());
        return concluido;
    }

    @Transactional
    public PlanoEstivaNavioDTO invalidarPlano(Long visitaId, Long planoId, ComandoMotivado comando) {
        PlanoEstivaNavio plano = buscarPlano(visitaId, planoId);
        if (plano.getStatus() == StatusPlanoEstivaNavio.INVALIDADO) {
            return dto(plano);
        }
        if (plano.getStatus() != StatusPlanoEstivaNavio.VALIDADO
                && plano.getStatus() != StatusPlanoEstivaNavio.CONCLUIDO) {
            throw new IllegalArgumentException("Somente plano validado ou publicado pode ser invalidado.");
        }

        StatusPlanoEstivaNavio anterior = plano.getStatus();
        plano.setStatus(StatusPlanoEstivaNavio.INVALIDADO);
        PlanoEstivaNavio salvo = planoRepositorio.save(plano);
        visitaServico.registrarEvento(
                plano.getVisitaNavio(),
                null,
                "PLANO_INVALIDADO",
                "Plano de estiva versao " + plano.getVersao() + " invalidado. Motivo: " + motivo(comando),
                usuario(comando),
                anterior.name(),
                StatusPlanoEstivaNavio.INVALIDADO.name());
        return dto(salvo);
    }

    @Transactional
    public PlanoEstivaNavioDTO cancelarPlano(Long visitaId, Long planoId, ComandoMotivado comando) {
        PlanoEstivaNavio plano = buscarPlano(visitaId, planoId);
        if (plano.getStatus() == StatusPlanoEstivaNavio.CANCELADO) {
            return dto(plano);
        }
        if (possuiExecucaoReal(planoId)) {
            throw new IllegalArgumentException("Nao e permitido cancelar plano com posicao ou item ja operado.");
        }

        StatusPlanoEstivaNavio anterior = plano.getStatus();
        plano.setStatus(StatusPlanoEstivaNavio.CANCELADO);
        PlanoEstivaNavio salvo = planoRepositorio.save(plano);
        visitaServico.registrarEvento(
                plano.getVisitaNavio(),
                null,
                "PLANO_CANCELADO",
                "Plano de estiva versao " + plano.getVersao() + " cancelado. Motivo: " + motivo(comando),
                usuario(comando),
                anterior.name(),
                StatusPlanoEstivaNavio.CANCELADO.name());
        return dto(salvo);
    }

    @Transactional
    public OrdemPatioDaVisitaDTO cancelarOrdem(Long visitaId, Long ordemId, ComandoMotivado comando) {
        VisitaNavio visita = visitaServico.buscarEntidade(visitaId);
        OrdemPatioYardRespostaDTO resposta = ordemComandoCliente.cancelar(ordemId, comando);
        validarOrdem(visitaId, ordemId, resposta);
        sincronizadorStatus.sincronizarStatus(visitaId);
        visitaServico.registrarEvento(
                visita,
                null,
                "ORDEM_PATIO_CANCELADA",
                "Ordem de patio " + ordemId + " cancelada. Motivo: " + motivo(comando),
                usuario(comando),
                null,
                resposta.getStatusOrdem());
        return converter(resposta);
    }

    private void cancelarOrdensDaVisitaComCompensacao(VisitaNavio visita, ComandoMotivado comando) {
        List<OrdemPatioYardRespostaDTO> ordens = ordemConsultaCliente.listarOrdensDaVisita(visita.getId());
        for (OrdemPatioYardRespostaDTO ordem : ordens) {
            cancelarOrdemComCompensacao(visita, ordem, comando);
        }
    }

    private void cancelarOrdensDoItemComCompensacao(ItemOperacaoNavio item, ComandoMotivado comando) {
        List<OrdemPatioYardRespostaDTO> ordens = ordemConsultaCliente.listarOrdensDaVisita(item.getVisitaNavio().getId());
        ordens.stream()
                .filter(ordem -> Objects.equals(ordem.getItemOperacaoNavioId(), item.getId()))
                .forEach(ordem -> cancelarOrdemComCompensacao(item.getVisitaNavio(), ordem, comando));
    }

    private void cancelarOrdemComCompensacao(
            VisitaNavio visita,
            OrdemPatioYardRespostaDTO ordem,
            ComandoMotivado comando) {
        if (ordem == null || ordem.getId() == null || statusTerminal(ordem.getStatusOrdem())) {
            return;
        }
        try {
            OrdemPatioYardRespostaDTO resposta = ordemComandoCliente.cancelar(ordem.getId(), comando);
            validarOrdem(visita.getId(), ordem.getId(), resposta);
            visitaServico.registrarEvento(
                    visita,
                    null,
                    "ORDEM_PATIO_CANCELADA",
                    "Ordem de patio " + ordem.getId() + " cancelada por compensacao administrativa.",
                    usuario(comando),
                    ordem.getStatusOrdem(),
                    resposta.getStatusOrdem());
        } catch (RuntimeException ex) {
            visitaServico.registrarEvento(
                    visita,
                    null,
                    "CANCELAMENTO_ORDEM_PATIO_PENDENTE",
                    "Cancelamento compensatorio da ordem de patio " + ordem.getId()
                            + " ficou pendente e deve ser reprocessado.",
                    usuario(comando),
                    ordem.getStatusOrdem(),
                    "PENDENTE_COMPENSACAO");
        }
    }

    private PlanoEstivaNavio buscarPlano(Long visitaId, Long planoId) {
        PlanoEstivaNavio plano = planoRepositorio.findById(planoId)
                .orElseThrow(() -> new IllegalArgumentException("Plano de estiva nao encontrado."));
        if (!Objects.equals(plano.getVisitaNavio().getId(), visitaId)) {
            throw new IllegalArgumentException("Plano de estiva nao pertence a visita informada.");
        }
        return plano;
    }

    private boolean possuiExecucaoReal(Long planoId) {
        return posicaoRepositorio.findByPlanoEstivaIdOrderBySequenciaAscIdAsc(planoId).stream()
                .anyMatch(posicao -> "OPERADO".equalsIgnoreCase(posicao.getStatus())
                        || posicao.getItemOperacao().getStatus() == StatusItemCarga.OPERADO);
    }

    private PlanoEstivaNavioDTO dto(PlanoEstivaNavio plano) {
        List<PosicaoEstivaNavioDTO> posicoes = posicaoRepositorio
                .findByPlanoEstivaIdOrderBySequenciaAscIdAsc(plano.getId()).stream()
                .map(PosicaoEstivaNavioDTO::de)
                .collect(Collectors.toList());
        return PlanoEstivaNavioDTO.de(plano, posicoes);
    }

    private void validarOrdem(Long visitaId, Long ordemId, OrdemPatioYardRespostaDTO resposta) {
        if (resposta == null || resposta.getId() == null) {
            throw new IllegalStateException("Servico-yard nao retornou a ordem de patio cancelada.");
        }
        if (!Objects.equals(ordemId, resposta.getId())) {
            throw new IllegalStateException("Servico-yard retornou uma ordem diferente da solicitada.");
        }
        if (resposta.getVisitaNavioId() != null && !Objects.equals(visitaId, resposta.getVisitaNavioId())) {
            throw new IllegalArgumentException("Ordem de patio nao pertence a visita informada.");
        }
    }

    private OrdemPatioDaVisitaDTO converter(OrdemPatioYardRespostaDTO ordem) {
        TipoMovimentoNavio tipoMovimento = tipoMovimentoNavio(ordem.getTipoMovimento());
        return new OrdemPatioDaVisitaDTO(
                ordem.getId(),
                ordem.getVisitaNavioId(),
                ordem.getItemOperacaoNavioId(),
                ordem.getCodigoConteiner(),
                tipoMovimento,
                ordem.getStatusOrdem(),
                tipoMovimento == TipoMovimentoNavio.DESCARGA ? "NAVIO" : ordem.getDestino(),
                tipoMovimento == TipoMovimentoNavio.EMBARQUE ? "NAVIO" : ordem.getDestino(),
                ordem.posicaoDestinoFormatada(),
                null,
                ordem.getSequenciaNavio(),
                ordem.getPrioridadeOperacional());
    }

    private TipoMovimentoNavio tipoMovimentoNavio(String tipoMovimentoPatio) {
        if (!StringUtils.hasText(tipoMovimentoPatio)) {
            return TipoMovimentoNavio.DESCARGA;
        }
        return switch (tipoMovimentoPatio.toUpperCase(Locale.ROOT)) {
            case "TRANSFERENCIA" -> TipoMovimentoNavio.EMBARQUE;
            case "REMANEJAMENTO" -> TipoMovimentoNavio.RESTOW;
            default -> TipoMovimentoNavio.DESCARGA;
        };
    }

    private boolean statusTerminal(String status) {
        return status != null && STATUS_ORDEM_TERMINAIS.contains(status.toUpperCase(Locale.ROOT));
    }

    private String motivo(ComandoMotivado comando) {
        if (comando == null || !StringUtils.hasText(comando.motivoNormalizado())) {
            throw new IllegalArgumentException("O motivo da operacao administrativa e obrigatorio.");
        }
        return comando.motivoNormalizado();
    }

    private String usuario(ComandoMotivado comando) {
        return comando == null ? "sistema" : comando.usuarioEfetivo("sistema");
    }
}
