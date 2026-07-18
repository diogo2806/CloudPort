package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.ConflictException;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.GateResourceOccupation;
import br.com.cloudport.servicogate.model.enums.GateResourceType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class GateResourceOccupationService {

    private final GateResourceOccupationRepository repository;

    public GateResourceOccupationService(GateResourceOccupationRepository repository) {
        this.repository = repository;
    }

    public List<GateResourceOccupation> ocuparRecursos(Agendamento agendamento,
                                                         GatePass gatePass,
                                                         String chassis,
                                                         List<String> unidades) {
        Map<GateResourceType, List<String>> recursos = montarRecursos(agendamento, chassis, unidades);
        List<GateResourceOccupation> ocupacoesAtivas = repository.findByGatePassIdAndAtivoTrue(gatePass.getId());
        if (!ocupacoesAtivas.isEmpty()) {
            validarOcupacaoExistente(gatePass, ocupacoesAtivas, recursos);
            return ocupacoesAtivas;
        }

        validarConflitos(recursos, gatePass.getId());

        LocalDateTime agora = LocalDateTime.now();
        List<GateResourceOccupation> ocupacoes = new ArrayList<>();
        recursos.forEach((tipo, chaves) -> chaves.forEach(chave -> {
            GateResourceOccupation ocupacao = new GateResourceOccupation();
            ocupacao.setGatePass(gatePass);
            ocupacao.setTipoRecurso(tipo);
            ocupacao.setChaveRecurso(chave);
            ocupacao.setAtivo(true);
            ocupacao.setOcupadoEm(agora);
            ocupacoes.add(ocupacao);
        }));

        try {
            return repository.saveAllAndFlush(ocupacoes);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    "Um dos recursos da visita foi ocupado por outra operação concorrente",
                    exception);
        }
    }

    public void liberarRecursos(Long gatePassId) {
        LocalDateTime agora = LocalDateTime.now();
        List<GateResourceOccupation> ocupacoes = repository.findByGatePassIdAndAtivoTrue(gatePassId);
        if (ocupacoes.isEmpty()) {
            return;
        }
        ocupacoes.forEach(ocupacao -> {
            ocupacao.setAtivo(false);
            ocupacao.setLiberadoEm(agora);
        });
        repository.saveAllAndFlush(ocupacoes);
    }

    @Transactional(readOnly = true)
    public List<GateResourceOccupation> listarAtivos(Long gatePassId) {
        return repository.findByGatePassIdAndAtivoTrue(gatePassId);
    }

    private Map<GateResourceType, List<String>> montarRecursos(Agendamento agendamento,
                                                                 String chassis,
                                                                 List<String> unidades) {
        Map<GateResourceType, List<String>> recursos = new LinkedHashMap<>();
        recursos.put(GateResourceType.MOTORISTA, List.of(
                normalizar(agendamento.getMotorista().getDocumento(), "Motorista")));
        recursos.put(GateResourceType.CAVALO, List.of(
                normalizar(agendamento.getVeiculo().getPlaca(), "Cavalo")));
        if (StringUtils.hasText(chassis)) {
            recursos.put(GateResourceType.CHASSIS, List.of(normalizar(chassis, "Chassis")));
        }
        List<String> unidadesNormalizadas = unidades == null ? List.of() : unidades.stream()
                .filter(StringUtils::hasText)
                .map(unidade -> normalizar(unidade, "Unidade"))
                .distinct()
                .collect(Collectors.toList());
        if (!unidadesNormalizadas.isEmpty()) {
            recursos.put(GateResourceType.UNIDADE, unidadesNormalizadas);
        }
        return recursos;
    }

    private void validarOcupacaoExistente(GatePass gatePass,
                                           List<GateResourceOccupation> ocupacoesAtivas,
                                           Map<GateResourceType, List<String>> recursosSolicitados) {
        Map<GateResourceType, Set<String>> recursosAtivos = ocupacoesAtivas.stream()
                .collect(Collectors.groupingBy(
                        GateResourceOccupation::getTipoRecurso,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                GateResourceOccupation::getChaveRecurso,
                                Collectors.toCollection(LinkedHashSet::new))));
        Map<GateResourceType, Set<String>> recursosNormalizados = recursosSolicitados.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new LinkedHashSet<>(entry.getValue()),
                        (existente, ignorado) -> existente,
                        LinkedHashMap::new));

        if (!recursosAtivos.equals(recursosNormalizados)) {
            throw new ConflictException(String.format(
                    "GatePass %s já possui visita ativa com recursos diferentes; finalize ou cancele a visita antes de alterá-los",
                    gatePass.getCodigo()));
        }
    }

    private void validarConflitos(Map<GateResourceType, List<String>> recursos, Long gatePassAtualId) {
        recursos.forEach((tipo, chaves) -> repository
                .findByTipoRecursoAndChaveRecursoInAndAtivoTrue(tipo, chaves)
                .stream()
                .filter(ocupacao -> !Objects.equals(ocupacao.getGatePass().getId(), gatePassAtualId))
                .findFirst()
                .ifPresent(ocupacao -> {
                    String gatePass = ocupacao.getGatePass().getCodigo();
                    throw new ConflictException(String.format(
                            "Conflito de visita ativa: recurso %s %s já está ocupado pelo GatePass %s",
                            tipo, ocupacao.getChaveRecurso(), gatePass));
                }));
    }

    private String normalizar(String valor, String campo) {
        if (!StringUtils.hasText(valor)) {
            throw new BusinessException(campo + " deve ser informado para validar a visita ativa");
        }
        return valor.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}
