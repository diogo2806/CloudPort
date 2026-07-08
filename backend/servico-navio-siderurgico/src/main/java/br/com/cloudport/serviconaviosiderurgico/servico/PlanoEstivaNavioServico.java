package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dominio.BordoEstivaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.PlanoEstivaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.PosicaoEstivaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusPlanoEstivaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.PlanoEstivaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PosicaoEstivaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ValidacaoPlanoEstivaDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.PlanoEstivaNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.PosicaoEstivaNavioRepositorio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanoEstivaNavioServico {

    private final PlanoEstivaNavioRepositorio planoRepositorio;
    private final PosicaoEstivaNavioRepositorio posicaoRepositorio;
    private final ItemOperacaoNavioRepositorio itemRepositorio;
    private final VisitaNavioServico visitaServico;

    public PlanoEstivaNavioServico(
            PlanoEstivaNavioRepositorio planoRepositorio,
            PosicaoEstivaNavioRepositorio posicaoRepositorio,
            ItemOperacaoNavioRepositorio itemRepositorio,
            VisitaNavioServico visitaServico
    ) {
        this.planoRepositorio = planoRepositorio;
        this.posicaoRepositorio = posicaoRepositorio;
        this.itemRepositorio = itemRepositorio;
        this.visitaServico = visitaServico;
    }

    @Transactional(readOnly = true)
    public PlanoEstivaNavioDTO obter(Long visitaId) {
        VisitaNavio visita = visitaServico.buscarEntidade(visitaId);
        PlanoEstivaNavio plano = planoRepositorio.findFirstByVisitaNavioIdOrderByVersaoDesc(visita.getId())
                .orElseThrow(() -> new IllegalArgumentException("Plano de estiva nao encontrado para a visita."));
        return dto(plano);
    }

    @Transactional
    public PlanoEstivaNavioDTO criar(Long visitaId, PlanoEstivaNavioDTO dto) {
        VisitaNavio visita = visitaServico.buscarEntidade(visitaId);
        visitaServico.validarVisitaEditavel(visita);
        int proximaVersao = planoRepositorio.findByVisitaNavioIdOrderByVersaoDesc(visitaId).stream()
                .findFirst()
                .map(plano -> plano.getVersao() + 1)
                .orElse(1);
        PlanoEstivaNavio plano = new PlanoEstivaNavio();
        plano.setVisitaNavio(visita);
        plano.setVersao(proximaVersao);
        PlanoEstivaNavio salvo = planoRepositorio.save(plano);
        if (dto != null && dto.posicoes() != null && !dto.posicoes().isEmpty()) {
            atualizarPosicoes(visitaId, salvo.getId(), dto.posicoes());
            salvo = planoRepositorio.findById(salvo.getId()).orElseThrow();
        }
        visitaServico.registrarEvento(visita, null, "PLANO_ESTIVA_CRIADO", "Plano de estiva versao " + salvo.getVersao() + " criado.", "sistema", null, String.valueOf(salvo.getId()));
        return dto(salvo);
    }

    @Transactional
    public PlanoEstivaNavioDTO atualizarPosicoes(Long visitaId, Long planoId, List<PosicaoEstivaNavioDTO> posicoes) {
        PlanoEstivaNavio plano = buscarPlanoDaVisita(visitaId, planoId);
        visitaServico.validarVisitaEditavel(plano.getVisitaNavio());
        if (plano.getStatus().encerrado()) {
            throw new IllegalArgumentException("Nao e permitido alterar plano de estiva encerrado.");
        }
        List<String> erros = validarPosicoesBasicas(plano, posicoes);
        if (!erros.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", erros));
        }
        posicaoRepositorio.deleteByPlanoEstivaId(planoId);
        List<PosicaoEstivaNavio> entidades = posicoes.stream()
                .map(posicao -> criarPosicao(plano, posicao))
                .collect(Collectors.toList());
        posicaoRepositorio.saveAll(entidades);
        recalcularPesos(plano);
        plano.setStatus(StatusPlanoEstivaNavio.RASCUNHO);
        PlanoEstivaNavio salvo = planoRepositorio.save(plano);
        visitaServico.registrarEvento(plano.getVisitaNavio(), null, "PLANO_POSICOES_ATUALIZADAS", "Posicoes do plano de estiva atualizadas.", "sistema", null, String.valueOf(entidades.size()));
        return dto(salvo);
    }

    @Transactional
    public ValidacaoPlanoEstivaDTO validar(Long visitaId, Long planoId) {
        PlanoEstivaNavio plano = buscarPlanoDaVisita(visitaId, planoId);
        ValidacaoResultado resultado = validarPlano(plano);
        recalcularPesos(plano);
        if (resultado.erros().isEmpty()) {
            plano.setStatus(StatusPlanoEstivaNavio.VALIDADO);
            plano.setValidadoEm(LocalDateTime.now());
            planoRepositorio.save(plano);
            visitaServico.registrarEvento(plano.getVisitaNavio(), null, "PLANO_VALIDADO", "Plano de estiva validado sem erros bloqueantes.", "sistema", null, plano.getStatus().name());
        }
        return new ValidacaoPlanoEstivaDTO(dto(plano), resultado.erros(), resultado.alertas());
    }

    @Transactional
    public PlanoEstivaNavioDTO concluir(Long visitaId, Long planoId) {
        PlanoEstivaNavio plano = buscarPlanoDaVisita(visitaId, planoId);
        ValidacaoResultado resultado = validarPlano(plano);
        if (!resultado.erros().isEmpty()) {
            throw new IllegalArgumentException("Plano possui erros bloqueantes: " + String.join(" ", resultado.erros()));
        }
        plano.setStatus(StatusPlanoEstivaNavio.CONCLUIDO);
        recalcularPesos(plano);
        PlanoEstivaNavio salvo = planoRepositorio.save(plano);
        visitaServico.registrarEvento(plano.getVisitaNavio(), null, "PLANO_CONCLUIDO", "Plano de estiva concluido.", "sistema", null, salvo.getStatus().name());
        return dto(salvo);
    }

    private PlanoEstivaNavio buscarPlanoDaVisita(Long visitaId, Long planoId) {
        PlanoEstivaNavio plano = planoRepositorio.findById(planoId).orElseThrow(() -> new IllegalArgumentException("Plano de estiva nao encontrado."));
        if (!Objects.equals(plano.getVisitaNavio().getId(), visitaId)) {
            throw new IllegalArgumentException("Plano de estiva nao pertence a visita informada.");
        }
        return plano;
    }

    private PosicaoEstivaNavio criarPosicao(PlanoEstivaNavio plano, PosicaoEstivaNavioDTO dto) {
        ItemOperacaoNavio item = itemRepositorio.findById(dto.itemOperacaoId()).orElseThrow(() -> new IllegalArgumentException("Item operacional nao encontrado para o plano."));
        PosicaoEstivaNavio posicao = new PosicaoEstivaNavio();
        posicao.setPlanoEstiva(plano);
        posicao.setItemOperacao(item);
        posicao.setPorao(dto.porao());
        posicao.setCamada(dto.camada());
        posicao.setColuna(dto.coluna());
        posicao.setBordo(dto.bordo());
        posicao.setSequencia(dto.sequencia());
        posicao.setPesoToneladas(dto.pesoToneladas());
        posicao.setStatus(dto.status() == null || dto.status().isBlank() ? "PLANEJADO" : dto.status().trim().toUpperCase());
        return posicao;
    }

    private List<String> validarPosicoesBasicas(PlanoEstivaNavio plano, List<PosicaoEstivaNavioDTO> posicoes) {
        List<String> erros = new ArrayList<>();
        Set<String> chavesPosicao = new HashSet<>();
        Set<Long> itens = new HashSet<>();
        for (PosicaoEstivaNavioDTO posicao : posicoes == null ? List.<PosicaoEstivaNavioDTO>of() : posicoes) {
            if (posicao.itemOperacaoId() == null || posicao.porao() == null || posicao.camada() == null || posicao.coluna() == null || posicao.bordo() == null || posicao.sequencia() == null || posicao.pesoToneladas() == null) {
                erros.add("Posicao de estiva incompleta.");
                continue;
            }
            if (posicao.porao() < 1 || posicao.camada() < 1 || posicao.coluna() < 1 || posicao.sequencia() < 1 || posicao.pesoToneladas().signum() <= 0) {
                erros.add("Porao, camada, coluna, sequencia e peso devem ser positivos.");
            }
            ItemOperacaoNavio item = itemRepositorio.findById(posicao.itemOperacaoId()).orElse(null);
            if (item == null) {
                erros.add("Item operacional " + posicao.itemOperacaoId() + " nao encontrado.");
            } else if (!Objects.equals(item.getVisitaNavio().getId(), plano.getVisitaNavio().getId())) {
                erros.add("Item " + item.getCodigoLote() + " nao pertence a visita do plano.");
            }
            String chavePosicao = posicao.porao() + "-" + posicao.camada() + "-" + posicao.coluna() + "-" + posicao.bordo();
            if (!chavesPosicao.add(chavePosicao)) {
                erros.add("Existe posicao de estiva duplicada: " + chavePosicao + ".");
            }
            if (!itens.add(posicao.itemOperacaoId())) {
                erros.add("Item operacional duplicado no plano: " + posicao.itemOperacaoId() + ".");
            }
        }
        return erros;
    }

    private ValidacaoResultado validarPlano(PlanoEstivaNavio plano) {
        List<String> erros = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        List<PosicaoEstivaNavio> posicoes = posicaoRepositorio.findByPlanoEstivaIdOrderBySequenciaAscIdAsc(plano.getId());
        if (posicoes.isEmpty()) {
            alertas.add("Plano sem posicoes de estiva cadastradas.");
        }
        Set<String> chaves = new HashSet<>();
        Set<Long> itens = new HashSet<>();
        Map<Integer, BigDecimal> pesoPorPorao = new HashMap<>();
        for (PosicaoEstivaNavio posicao : posicoes) {
            String chave = posicao.getPorao() + "-" + posicao.getCamada() + "-" + posicao.getColuna() + "-" + posicao.getBordo();
            if (!chaves.add(chave) && !"CANCELADO".equalsIgnoreCase(posicao.getStatus())) {
                erros.add("Posicao ativa duplicada no plano: " + chave + ".");
            }
            if (!itens.add(posicao.getItemOperacao().getId())) {
                erros.add("Item duplicado no plano: " + posicao.getItemOperacao().getCodigoLote() + ".");
            }
            if (posicao.getItemOperacao().getStatus() == StatusItemCarga.BLOQUEADO) {
                erros.add("Item bloqueado no plano: " + posicao.getItemOperacao().getCodigoLote() + ".");
            }
            pesoPorPorao.merge(posicao.getPorao(), posicao.getPesoToneladas(), BigDecimal::add);
        }
        validarPesoPorPorao(plano, pesoPorPorao, erros, alertas);
        return new ValidacaoResultado(erros, alertas);
    }

    private void validarPesoPorPorao(PlanoEstivaNavio plano, Map<Integer, BigDecimal> pesoPorPorao, List<String> erros, List<String> alertas) {
        if (plano.getVisitaNavio().getNavio().getDwtToneladas() == null || plano.getVisitaNavio().getNavio().getQuantidadePoroes() == null || plano.getVisitaNavio().getNavio().getQuantidadePoroes() <= 0) {
            alertas.add("Limite de peso por porao nao configurado no navio.");
            return;
        }
        BigDecimal limiteEstimado = plano.getVisitaNavio().getNavio().getDwtToneladas()
                .divide(BigDecimal.valueOf(plano.getVisitaNavio().getNavio().getQuantidadePoroes()), 3, RoundingMode.HALF_UP);
        for (Map.Entry<Integer, BigDecimal> entrada : pesoPorPorao.entrySet()) {
            if (entrada.getValue().compareTo(limiteEstimado) > 0) {
                erros.add("Peso planejado do porao " + entrada.getKey() + " excede o limite estimado de " + limiteEstimado + " t.");
            } else if (entrada.getValue().compareTo(limiteEstimado.multiply(BigDecimal.valueOf(0.8))) > 0) {
                alertas.add("Peso planejado do porao " + entrada.getKey() + " acima de 80% do limite estimado.");
            }
        }
    }

    private void recalcularPesos(PlanoEstivaNavio plano) {
        List<PosicaoEstivaNavio> posicoes = posicaoRepositorio.findByPlanoEstivaIdOrderBySequenciaAscIdAsc(plano.getId());
        BigDecimal planejado = posicoes.stream()
                .map(PosicaoEstivaNavio::getPesoToneladas)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal realizado = posicoes.stream()
                .filter(posicao -> "OPERADO".equalsIgnoreCase(posicao.getStatus()) || posicao.getItemOperacao().getStatus() == StatusItemCarga.OPERADO)
                .map(PosicaoEstivaNavio::getPesoToneladas)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        plano.setPesoTotalPlanejado(planejado);
        plano.setPesoTotalRealizado(realizado);
    }

    private PlanoEstivaNavioDTO dto(PlanoEstivaNavio plano) {
        List<PosicaoEstivaNavioDTO> posicoes = posicaoRepositorio.findByPlanoEstivaIdOrderBySequenciaAscIdAsc(plano.getId()).stream()
                .map(PosicaoEstivaNavioDTO::de)
                .collect(Collectors.toList());
        return PlanoEstivaNavioDTO.de(plano, posicoes);
    }

    private record ValidacaoResultado(List<String> erros, List<String> alertas) {}
}
