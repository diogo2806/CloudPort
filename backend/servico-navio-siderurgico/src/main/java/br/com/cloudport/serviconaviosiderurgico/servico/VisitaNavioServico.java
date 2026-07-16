package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.configuracao.CorrelationIdFilter;
import br.com.cloudport.serviconaviosiderurgico.dominio.EventoVisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.FaseVisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.NavioSiderurgico;
import br.com.cloudport.serviconaviosiderurgico.dominio.ReservaPosicaoPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusIntegracaoPatio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.EventoVisitaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ResumoOperacionalNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.VisitaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.EventoVisitaNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ReservaPosicaoPatioNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.VisitaNavioRepositorio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class VisitaNavioServico {

    private final VisitaNavioRepositorio visitaRepositorio;
    private final ItemOperacaoNavioRepositorio itemRepositorio;
    private final EventoVisitaNavioRepositorio eventoRepositorio;
    private final ReservaPosicaoPatioNavioRepositorio reservaRepositorio;
    private final NavioSiderurgicoServico navioServico;
    private final EventoIntegracaoPublicador eventoPublicador;

    public VisitaNavioServico(
            VisitaNavioRepositorio visitaRepositorio,
            ItemOperacaoNavioRepositorio itemRepositorio,
            EventoVisitaNavioRepositorio eventoRepositorio,
            ReservaPosicaoPatioNavioRepositorio reservaRepositorio,
            NavioSiderurgicoServico navioServico,
            EventoIntegracaoPublicador eventoPublicador
    ) {
        this.visitaRepositorio = visitaRepositorio;
        this.itemRepositorio = itemRepositorio;
        this.eventoRepositorio = eventoRepositorio;
        this.reservaRepositorio = reservaRepositorio;
        this.navioServico = navioServico;
        this.eventoPublicador = eventoPublicador;
    }

    @Transactional(readOnly = true)
    public List<VisitaNavioDTO> listar(FaseVisitaNavio fase,
                                       LocalDateTime dataInicio,
                                       LocalDateTime dataFim,
                                       Long navioId) {
        return visitaRepositorio.findAllByOrderByEtaDesc().stream()
                .filter(visita -> fase == null || visita.getFase() == fase)
                .filter(visita -> navioId == null || Objects.equals(visita.getNavio().getId(), navioId))
                .filter(visita -> dataInicio == null || visita.getEta() == null || !visita.getEta().isBefore(dataInicio))
                .filter(visita -> dataFim == null || visita.getEta() == null || !visita.getEta().isAfter(dataFim))
                .map(VisitaNavioDTO::de)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VisitaNavioDTO detalhar(Long id) {
        return VisitaNavioDTO.de(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public VisitaNavio buscarEntidade(Long id) {
        return visitaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Visita de navio nao encontrada."));
    }

    @Transactional
    public VisitaNavioDTO criar(VisitaNavioDTO dto) {
        String codigo = normalizarObrigatorio(dto.codigoVisita(), "Codigo da visita e obrigatorio.");
        if (visitaRepositorio.existsByCodigoVisitaIgnoreCase(codigo)) {
            throw new IllegalArgumentException("Ja existe visita de navio com este codigo.");
        }
        validarDatas(dto);
        VisitaNavio visita = new VisitaNavio();
        visita.setNavio(navioServico.buscarEntidade(dto.navioId()));
        preencher(visita, dto, codigo);
        VisitaNavio salva = visitaRepositorio.save(visita);
        registrarEvento(salva, null, "VISITA_CRIADA", "Visita criada.", "sistema", null, codigo);
        return VisitaNavioDTO.de(salva);
    }

    @Transactional
    public VisitaNavioDTO atualizar(Long id, VisitaNavioDTO dto) {
        VisitaNavio visita = buscarEntidade(id);
        validarVisitaEditavel(visita);
        String codigo = normalizarObrigatorio(dto.codigoVisita(), "Codigo da visita e obrigatorio.");
        visitaRepositorio.findByCodigoVisitaIgnoreCase(codigo)
                .filter(existente -> !Objects.equals(existente.getId(), id))
                .ifPresent(existente -> {
                    throw new IllegalArgumentException("Ja existe visita de navio com este codigo.");
                });
        validarDatas(dto);
        if (!Objects.equals(visita.getNavio().getId(), dto.navioId())) {
            NavioSiderurgico navio = navioServico.buscarEntidade(dto.navioId());
            visita.setNavio(navio);
        }
        preencher(visita, dto, codigo);
        VisitaNavio salva = visitaRepositorio.save(visita);
        registrarEvento(salva, null, "VISITA_ATUALIZADA", "Dados da visita atualizados.", "sistema", null, codigo);
        return VisitaNavioDTO.de(salva);
    }

    @Transactional
    public VisitaNavioDTO alterarFase(Long id,
                                      FaseVisitaNavio novaFase,
                                      String usuario,
                                      String observacao) {
        VisitaNavio visita = buscarEntidade(id);
        FaseVisitaNavio faseAnterior = visita.getFase();
        if (!faseAnterior.permiteTransicaoPara(novaFase)) {
            throw new IllegalArgumentException("Transicao de fase invalida: " + faseAnterior + " para " + novaFase + ".");
        }
        visita.setFase(novaFase);
        LocalDateTime agora = LocalDateTime.now();
        if (novaFase == FaseVisitaNavio.ATRACADA && visita.getAtb() == null) {
            visita.setAtb(agora);
        } else if (novaFase == FaseVisitaNavio.OPERANDO && visita.getInicioOperacao() == null) {
            visita.setInicioOperacao(agora);
        } else if (novaFase == FaseVisitaNavio.OPERACAO_CONCLUIDA && visita.getFimOperacao() == null) {
            visita.setFimOperacao(agora);
        } else if (novaFase == FaseVisitaNavio.PARTIU && visita.getAtd() == null) {
            visita.setAtd(agora);
        }
        VisitaNavio salva = visitaRepositorio.save(visita);
        String descricao = observacao == null || observacao.isBlank()
                ? "Fase alterada de " + faseAnterior + " para " + novaFase + "."
                : observacao.trim();
        registrarEvento(salva, null, "FASE_ALTERADA", descricao, usuario, faseAnterior.name(), novaFase.name());
        if (novaFase == FaseVisitaNavio.CANCELADA) {
            cancelarReservasAtivas(salva, descricao, usuario);
        }
        return VisitaNavioDTO.de(salva);
    }

    @Transactional
    public void excluir(Long id) {
        VisitaNavio visita = buscarEntidade(id);
        if (visita.getInicioOperacao() != null
                || visita.getFase() == FaseVisitaNavio.OPERANDO
                || visita.getFase() == FaseVisitaNavio.OPERACAO_CONCLUIDA
                || visita.getFase() == FaseVisitaNavio.PARTIU) {
            throw new IllegalArgumentException("Nao e permitido excluir visita com operacao iniciada.");
        }
        if (itemRepositorio.countByVisitaNavioId(id) > 0) {
            throw new IllegalArgumentException("Nao e permitido excluir visita com itens operacionais vinculados.");
        }
        visitaRepositorio.delete(visita);
    }

    @Transactional(readOnly = true)
    public ResumoOperacionalNavioDTO resumo(Long visitaId) {
        VisitaNavio visita = buscarEntidade(visitaId);
        List<ItemOperacaoNavio> itens = itemRepositorio.findByVisitaNavioId(visitaId);
        BigDecimal pesoPlanejado = itens.stream()
                .map(ItemOperacaoNavio::getPesoTotalToneladas)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pesoOperado = itens.stream()
                .filter(item -> item.getStatus() == StatusItemCarga.OPERADO)
                .map(ItemOperacaoNavio::getPesoTotalToneladas)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long operados = itens.stream().filter(item -> item.getStatus() == StatusItemCarga.OPERADO).count();
        long bloqueados = itens.stream().filter(item -> item.getStatus() == StatusItemCarga.BLOQUEADO).count();
        long divergencias = itens.stream().filter(this::temDivergencia).count();
        int percentual = pesoPlanejado.signum() == 0 ? 0 : pesoOperado.multiply(BigDecimal.valueOf(100))
                .divide(pesoPlanejado, 0, RoundingMode.HALF_UP).intValue();
        Long tempo = visita.getInicioOperacao() == null ? null : Duration.between(
                visita.getInicioOperacao(),
                visita.getFimOperacao() == null ? LocalDateTime.now() : visita.getFimOperacao()
        ).toMinutes();
        return new ResumoOperacionalNavioDTO(
                itens.size(),
                operados,
                pesoPlanejado,
                pesoOperado,
                percentual,
                divergencias,
                bloqueados,
                tempo
        );
    }

    @Transactional(readOnly = true)
    public List<EventoVisitaNavioDTO> eventos(Long visitaId) {
        buscarEntidade(visitaId);
        return eventoRepositorio.findByVisitaNavioIdOrderByCriadoEmDesc(visitaId).stream()
                .map(EventoVisitaNavioDTO::de)
                .collect(Collectors.toList());
    }

    public void validarVisitaEditavel(VisitaNavio visita) {
        if (visita.getFase() == FaseVisitaNavio.PARTIU || visita.getFase() == FaseVisitaNavio.CANCELADA) {
            throw new IllegalArgumentException("Nao e permitido alterar visita em fase " + visita.getFase() + ".");
        }
    }

    public void registrarEvento(VisitaNavio visita,
                                 ItemOperacaoNavio item,
                                 String tipo,
                                 String descricao,
                                 String usuario,
                                 String antes,
                                 String depois) {
        EventoVisitaNavio evento = new EventoVisitaNavio();
        evento.setVisitaNavio(visita);
        evento.setItemOperacao(item);
        evento.setTipoEvento(tipo);
        evento.setDescricao(descricao);
        evento.setUsuario(usuario == null || usuario.isBlank() ? "sistema" : usuario.trim());
        evento.setDadosAntes(antes);
        evento.setDadosDepois(depois);
        EventoVisitaNavio salvo = eventoRepositorio.save(evento);
        eventoPublicador.publicar(visita.getId(), EventoVisitaNavioDTO.de(salvo), correlationIdAtual());
    }

    private void cancelarReservasAtivas(VisitaNavio visita, String motivo, String usuario) {
        List<ReservaPosicaoPatioNavio> reservas = reservaRepositorio.findByVisitaNavioIdAndStatusOrderByCriadoEmAsc(
                visita.getId(),
                StatusReservaPatioNavio.ATIVA
        );
        for (ReservaPosicaoPatioNavio reserva : reservas) {
            reserva.setStatus(StatusReservaPatioNavio.CANCELADA);
            reserva.setMotivoCancelamento("Visita cancelada: " + motivo);
            reservaRepositorio.save(reserva);
            ItemOperacaoNavio item = itemRepositorio.findById(reserva.getItemOperacaoNavioId()).orElse(null);
            if (item != null && item.getStatus() != StatusItemCarga.OPERADO) {
                item.setStatus(StatusItemCarga.CANCELADO);
                item.setStatusIntegracaoPatio(StatusIntegracaoPatio.CANCELADO);
                itemRepositorio.save(item);
            }
            registrarEvento(
                    visita,
                    item,
                    "RESERVA_PATIO_CANCELADA",
                    "Reserva " + reserva.getId() + " da posicao " + reserva.getPosicaoPatioId()
                            + " cancelada devido ao cancelamento da visita.",
                    usuario,
                    StatusReservaPatioNavio.ATIVA.name(),
                    StatusReservaPatioNavio.CANCELADA.name()
            );
        }
    }

    private String correlationIdAtual() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos instanceof ServletRequestAttributes servletRequestAttributes) {
            return CorrelationIdFilter.obter(servletRequestAttributes.getRequest());
        }
        return null;
    }

    private void preencher(VisitaNavio visita, VisitaNavioDTO dto, String codigo) {
        visita.setCodigoVisita(codigo);
        visita.setViagemEntrada(normalizarOpcional(dto.viagemEntrada()));
        visita.setViagemSaida(normalizarOpcional(dto.viagemSaida()));
        visita.setLinhaOperadora(limpar(dto.linhaOperadora()));
        visita.setTerminalFacility(limpar(dto.terminalFacility()));
        visita.setBercoPrevisto(limpar(dto.bercoPrevisto()));
        visita.setBercoAtual(limpar(dto.bercoAtual()));
        visita.setEta(dto.eta());
        visita.setAta(dto.ata());
        visita.setEtb(dto.etb());
        visita.setAtb(dto.atb());
        visita.setInicioOperacao(dto.inicioOperacao());
        visita.setFimOperacao(dto.fimOperacao());
        visita.setEtd(dto.etd());
        visita.setAtd(dto.atd());
        visita.setJanelaRecebimentoInicio(dto.janelaRecebimentoInicio());
        visita.setJanelaRecebimentoFim(dto.janelaRecebimentoFim());
        visita.setCutoffOperacional(dto.cutoffOperacional());
        visita.setFase(dto.fase() == null ? FaseVisitaNavio.PREVISTA : dto.fase());
        visita.setObservacoes(limpar(dto.observacoes()));
    }

    private void validarDatas(VisitaNavioDTO dto) {
        validarOrdem(dto.eta(), dto.ata(), "ATA nao pode ser anterior ao ETA.");
        validarOrdem(dto.eta(), dto.etb(), "ETB nao pode ser anterior ao ETA.");
        validarOrdem(dto.etb(), dto.atb(), "ATB nao pode ser anterior ao ETB.");
        validarOrdem(dto.atb(), dto.inicioOperacao(), "Inicio da operacao nao pode ser anterior ao ATB.");
        validarOrdem(dto.inicioOperacao(), dto.fimOperacao(), "Fim da operacao nao pode ser anterior ao inicio.");
        validarOrdem(dto.eta(), dto.etd(), "ETD nao pode ser anterior ao ETA.");
        validarOrdem(dto.etd(), dto.atd(), "ATD nao pode ser anterior ao ETD.");
        validarOrdem(
                dto.janelaRecebimentoInicio(),
                dto.janelaRecebimentoFim(),
                "Fim da janela de recebimento nao pode ser anterior ao inicio."
        );
    }

    private void validarOrdem(LocalDateTime inicio, LocalDateTime fim, String mensagem) {
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    private boolean temDivergencia(ItemOperacaoNavio item) {
        boolean poraoDiferente = item.getPoraoPlanejado() != null
                && item.getPoraoReal() != null
                && !Objects.equals(item.getPoraoPlanejado(), item.getPoraoReal());
        boolean posicaoDiferente = item.getPosicaoPlanejada() != null
                && item.getPosicaoReal() != null
                && !item.getPosicaoPlanejada().equalsIgnoreCase(item.getPosicaoReal());
        return poraoDiferente || posicaoDiferente;
    }

    private String normalizarObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
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
