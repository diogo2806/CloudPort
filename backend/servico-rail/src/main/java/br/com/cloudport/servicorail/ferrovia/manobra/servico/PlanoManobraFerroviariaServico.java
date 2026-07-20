package br.com.cloudport.servicorail.ferrovia.manobra.servico;

import br.com.cloudport.servicorail.ferrovia.manobra.dto.PlanoManobraFerroviariaDto.AlteracaoStatus;
import br.com.cloudport.servicorail.ferrovia.manobra.dto.PlanoManobraFerroviariaDto.Criacao;
import br.com.cloudport.servicorail.ferrovia.manobra.dto.PlanoManobraFerroviariaDto.Resposta;
import br.com.cloudport.servicorail.ferrovia.manobra.modelo.PlanoManobraFerroviaria;
import br.com.cloudport.servicorail.ferrovia.manobra.modelo.PlanoManobraFerroviaria.StatusPlanoManobra;
import br.com.cloudport.servicorail.ferrovia.manobra.repositorio.PlanoManobraFerroviariaRepositorio;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import br.com.cloudport.servicorail.ferrovia.repositorio.VisitaTremRepositorio;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlanoManobraFerroviariaServico {

    private static final EnumSet<StatusPlanoManobra> STATUS_QUE_RESERVAM_TRECHO = EnumSet.of(
            StatusPlanoManobra.PLANEJADA,
            StatusPlanoManobra.AUTORIZADA,
            StatusPlanoManobra.EM_EXECUCAO);

    private final PlanoManobraFerroviariaRepositorio planoRepositorio;
    private final VisitaTremRepositorio visitaTremRepositorio;

    public PlanoManobraFerroviariaServico(PlanoManobraFerroviariaRepositorio planoRepositorio,
                                           VisitaTremRepositorio visitaTremRepositorio) {
        this.planoRepositorio = planoRepositorio;
        this.visitaTremRepositorio = visitaTremRepositorio;
    }

    @Transactional(readOnly = true)
    public List<Resposta> listar(Long idVisita) {
        validarId(idVisita, "visita");
        if (!visitaTremRepositorio.existsById(idVisita)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita de trem não encontrada.");
        }
        return planoRepositorio.findByVisitaTremIdOrderBySequenciaAsc(idVisita)
                .stream()
                .map(Resposta::deEntidade)
                .collect(Collectors.toList());
    }

    @Transactional
    public Resposta criar(Long idVisita, Criacao dto) {
        validarId(idVisita, "visita");
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Os dados da manobra devem ser informados.");
        }
        if (dto.getFimPrevisto() == null || dto.getInicioPrevisto() == null
                || !dto.getFimPrevisto().isAfter(dto.getInicioPrevisto())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O fim previsto deve ser posterior ao início previsto.");
        }
        if (planoRepositorio.existsByVisitaTremIdAndSequencia(idVisita, dto.getSequencia())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma manobra com a sequência informada para esta visita.");
        }

        VisitaTrem visita = visitaTremRepositorio.findOneById(idVisita)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Visita de trem não encontrada."));

        PlanoManobraFerroviaria plano = new PlanoManobraFerroviaria();
        plano.setVisitaTrem(visita);
        plano.setSequencia(dto.getSequencia());
        plano.setOrigem(normalizar(dto.getOrigem()));
        plano.setDestino(normalizar(dto.getDestino()));
        plano.setComposicao(normalizar(dto.getComposicao()));
        plano.setLinha(normalizarMaiusculo(dto.getLinha()));
        plano.setTrecho(normalizarMaiusculo(dto.getTrecho()));
        plano.setInicioPrevisto(dto.getInicioPrevisto());
        plano.setFimPrevisto(dto.getFimPrevisto());

        Optional<PlanoManobraFerroviaria> conflito = localizarConflito(plano, null);
        if (conflito.isPresent()) {
            plano.setStatus(StatusPlanoManobra.BLOQUEADA_CONFLITO);
            plano.setConflitoDescricao(descreverConflito(conflito.get()));
        } else {
            plano.setStatus(StatusPlanoManobra.PLANEJADA);
        }

        try {
            return Resposta.deEntidade(planoRepositorio.saveAndFlush(plano));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O trecho foi reservado por outra manobra durante a confirmação. Recarregue o plano.",
                    exception);
        }
    }

    @Transactional
    public Resposta alterarStatus(Long idVisita,
                                  Long idManobra,
                                  AlteracaoStatus dto,
                                  String usuarioAutenticado) {
        validarId(idVisita, "visita");
        validarId(idManobra, "manobra");
        if (dto == null || dto.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O novo status da manobra deve ser informado.");
        }

        PlanoManobraFerroviaria plano = planoRepositorio.findByIdAndVisitaTremId(idManobra, idVisita)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Manobra ferroviária não encontrada para a visita informada."));
        StatusPlanoManobra atual = plano.getStatus();
        StatusPlanoManobra destino = dto.getStatus();
        if (Objects.equals(atual, destino)) {
            return Resposta.deEntidade(plano);
        }

        LocalDateTime agora = LocalDateTime.now();
        if (destino == StatusPlanoManobra.AUTORIZADA
                && (atual == StatusPlanoManobra.PLANEJADA || atual == StatusPlanoManobra.BLOQUEADA_CONFLITO)) {
            Optional<PlanoManobraFerroviaria> conflito = localizarConflito(plano, plano.getId());
            if (conflito.isPresent()) {
                plano.setStatus(StatusPlanoManobra.BLOQUEADA_CONFLITO);
                plano.setConflitoDescricao(descreverConflito(conflito.get()));
                return Resposta.deEntidade(planoRepositorio.save(plano));
            }
            plano.setStatus(StatusPlanoManobra.AUTORIZADA);
            plano.setConflitoDescricao(null);
            plano.setAutorizadoPor(usuario(usuarioAutenticado));
            plano.setAutorizadoEm(agora);
        } else if (destino == StatusPlanoManobra.EM_EXECUCAO
                && atual == StatusPlanoManobra.AUTORIZADA) {
            plano.setStatus(StatusPlanoManobra.EM_EXECUCAO);
            plano.setIniciadoEm(agora);
        } else if (destino == StatusPlanoManobra.CONCLUIDA
                && atual == StatusPlanoManobra.EM_EXECUCAO) {
            plano.setStatus(StatusPlanoManobra.CONCLUIDA);
            plano.setConcluidoEm(agora);
        } else if (destino == StatusPlanoManobra.CANCELADA
                && EnumSet.of(StatusPlanoManobra.PLANEJADA,
                        StatusPlanoManobra.BLOQUEADA_CONFLITO,
                        StatusPlanoManobra.AUTORIZADA).contains(atual)) {
            if (!StringUtils.hasText(dto.getMotivo())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O motivo do cancelamento deve ser informado.");
            }
            plano.setStatus(StatusPlanoManobra.CANCELADA);
            plano.setMotivoCancelamento(normalizar(dto.getMotivo()));
        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format(Locale.ROOT,
                            "A transição da manobra de %s para %s não é permitida.", atual, destino));
        }

        try {
            return Resposta.deEntidade(planoRepositorio.saveAndFlush(plano));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não foi possível reservar o trecho porque existe outra manobra ativa no período.",
                    exception);
        }
    }

    private Optional<PlanoManobraFerroviaria> localizarConflito(PlanoManobraFerroviaria candidato,
                                                                 Long idIgnorado) {
        return planoRepositorio.findByLinhaIgnoreCaseAndStatusInOrderByInicioPrevistoAsc(
                        candidato.getLinha(), STATUS_QUE_RESERVAM_TRECHO)
                .stream()
                .filter(existente -> !Objects.equals(existente.getId(), idIgnorado))
                .filter(existente -> candidato.getTrecho().equalsIgnoreCase(existente.getTrecho()))
                .filter(existente -> candidato.getInicioPrevisto().isBefore(existente.getFimPrevisto())
                        && candidato.getFimPrevisto().isAfter(existente.getInicioPrevisto()))
                .findFirst();
    }

    private String descreverConflito(PlanoManobraFerroviaria conflito) {
        return String.format(Locale.ROOT,
                "Conflito com a manobra %d, sequência %d, reservada de %s até %s.",
                conflito.getId(),
                conflito.getSequencia(),
                conflito.getInicioPrevisto(),
                conflito.getFimPrevisto());
    }

    private void validarId(Long id, String campo) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O identificador da %s é inválido.", campo));
        }
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String normalizarMaiusculo(String valor) {
        String normalizado = normalizar(valor);
        return normalizado == null ? null : normalizado.toUpperCase(Locale.ROOT);
    }

    private String usuario(String usuarioAutenticado) {
        return StringUtils.hasText(usuarioAutenticado) ? usuarioAutenticado.trim() : "USUARIO_AUTENTICADO";
    }
}
