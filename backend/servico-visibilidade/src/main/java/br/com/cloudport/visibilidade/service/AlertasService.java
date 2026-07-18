package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.dto.AlertaResumoDTO;
import br.com.cloudport.visibilidade.entity.Alerta;
import br.com.cloudport.visibilidade.entity.CapacidadeYard;
import br.com.cloudport.visibilidade.entity.StatusNavio;
import br.com.cloudport.visibilidade.repository.AlertaRepository;
import br.com.cloudport.visibilidade.repository.CapacidadeYardRepository;
import br.com.cloudport.visibilidade.repository.StatusNavioRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AlertasService {

    private static final String STATUS_ATIVO = "ativo";
    private static final String STATUS_RESOLVIDO = "resolvido";
    private static final String USUARIO_SISTEMA = "sistema";

    private final AlertaRepository alertaRepository;
    private final StatusNavioRepository statusNavioRepository;
    private final CapacidadeYardRepository capacidadeYardRepository;

    public AlertasService(AlertaRepository alertaRepository,
                          StatusNavioRepository statusNavioRepository,
                          CapacidadeYardRepository capacidadeYardRepository) {
        this.alertaRepository = alertaRepository;
        this.statusNavioRepository = statusNavioRepository;
        this.capacidadeYardRepository = capacidadeYardRepository;
    }

    @Transactional(readOnly = true)
    public List<Alerta> listarAlertasAtivos() {
        return alertaRepository.findByStatusOrderByDataGeradaDesc(STATUS_ATIVO);
    }

    @Transactional(readOnly = true)
    public Page<Alerta> buscarAlertasFiltrados(List<String> severidades,
                                               List<String> tipos,
                                               String status,
                                               Pageable pageable) {
        Specification<Alerta> specification = Specification.where(null);
        String statusNormalizado = normalizar(status);
        List<String> severidadesNormalizadas = normalizarLista(severidades, false);
        List<String> tiposNormalizados = normalizarLista(tipos, true);

        if (StringUtils.hasText(statusNormalizado)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(criteriaBuilder.lower(root.<String>get("status")), statusNormalizado));
        }
        if (!severidadesNormalizadas.isEmpty()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lower(root.<String>get("severidade")).in(severidadesNormalizadas));
        }
        if (!tiposNormalizados.isEmpty()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.upper(root.<String>get("tipo")).in(tiposNormalizados));
        }
        return alertaRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public AlertaResumoDTO obterResumoAtivos() {
        return new AlertaResumoDTO(
                alertaRepository.countByStatus(STATUS_ATIVO),
                alertaRepository.countByStatusAndSeveridadeIgnoreCase(STATUS_ATIVO, "critica"),
                alertaRepository.countByStatusAndSeveridadeIgnoreCase(STATUS_ATIVO, "alta"),
                alertaRepository.countByStatusAndSeveridadeIgnoreCase(STATUS_ATIVO, "media"),
                alertaRepository.countByStatusAndSeveridadeIgnoreCase(STATUS_ATIVO, "baixa"),
                alertaRepository.countByStatusAndDataReconhecimentoIsNull(STATUS_ATIVO));
    }

    @Transactional
    public Alerta criarAlerta(String tipo,
                              String severidade,
                              String entidadeId,
                              String descricao,
                              String acaoSugerida) {
        Alerta alerta = new Alerta();
        alerta.setTipo(tipo);
        alerta.setSeveridade(severidade);
        alerta.setEntidadeId(entidadeId);
        alerta.setDescricao(descricao);
        alerta.setDataGerada(LocalDateTime.now());
        alerta.setStatus(STATUS_ATIVO);
        alerta.setAcaoSugerida(acaoSugerida);
        return alertaRepository.save(alerta);
    }

    @Transactional
    public Alerta reconhecerAlerta(Long id, String usuario) {
        Alerta alerta = buscarAlerta(id);
        if (alerta.getDataReconhecimento() == null) {
            alerta.setDataReconhecimento(LocalDateTime.now());
            alerta.setReconhecidoPor(normalizarUsuario(usuario));
            return alertaRepository.save(alerta);
        }
        return alerta;
    }

    @Transactional
    public Alerta resolverAlerta(Long id) {
        return resolverAlerta(id, USUARIO_SISTEMA);
    }

    @Transactional
    public Alerta resolverAlerta(Long id, String usuario) {
        Alerta alerta = buscarAlerta(id);
        return resolver(alerta, usuario);
    }

    @Transactional
    public void resolverAlertasAtivos(String entidadeId, String tipo) {
        if (!StringUtils.hasText(entidadeId) || !StringUtils.hasText(tipo)) {
            return;
        }

        alertaRepository.findByEntidadeIdAndStatus(entidadeId, STATUS_ATIVO).stream()
                .filter(alerta -> tipo.equalsIgnoreCase(alerta.getTipo()))
                .forEach(alerta -> resolver(alerta, USUARIO_SISTEMA));
    }

    @Transactional
    public void detectarAtrasos() {
        statusNavioRepository.findAll().forEach(navio -> {
            Integer atraso = navio.getAtrasoMinutos();
            if (atraso == null || atraso <= 30) {
                resolverAlertasAtivos(navio.getNavioId(), "ATRASO_NAVIO");
                return;
            }

            List<Alerta> alertasAtivos = alertaRepository.findByEntidadeIdAndStatus(navio.getNavioId(), STATUS_ATIVO);
            alertasAtivos.stream()
                    .filter(alerta -> "ATRASO_NAVIO".equalsIgnoreCase(alerta.getTipo()))
                    .findFirst()
                    .ifPresentOrElse(alerta -> atualizarAlertaAtraso(alerta, navio, atraso),
                            () -> criarAlertaAtraso(navio, atraso));
        });
    }

    @Transactional
    public void detectarGargalos() {
        capacidadeYardRepository.findAll().forEach(zona -> {
            double percentual = calcularPercentual(zona);
            String entidadeId = "YARD-" + zona.getZona();
            List<Alerta> alertasAtivos = alertaRepository.findByEntidadeIdAndStatus(entidadeId, STATUS_ATIVO);
            if (percentual < 85d) {
                resolverAlertasAtivos(entidadeId, "GARGALO_YARD");
                return;
            }

            alertasAtivos.stream()
                    .filter(alerta -> "GARGALO_YARD".equalsIgnoreCase(alerta.getTipo()))
                    .findFirst()
                    .ifPresentOrElse(alerta -> atualizarAlertaGargalo(alerta, zona, percentual),
                            () -> criarAlertaGargalo(zona, percentual));
        });
    }

    private Alerta buscarAlerta(Long id) {
        return alertaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alerta nao encontrado: " + id));
    }

    private void criarAlertaAtraso(StatusNavio navio, int atraso) {
        Alerta alerta = new Alerta();
        alerta.setTipo("ATRASO_NAVIO");
        alerta.setSeveridade(atraso >= 360 ? "critica" : atraso >= 120 ? "alta" : "media");
        alerta.setEntidadeId(navio.getNavioId());
        alerta.setDescricao("Navio " + navio.getNomeNavio() + " atrasado em " + atraso + " minutos");
        alerta.setDataGerada(LocalDateTime.now());
        alerta.setStatus(STATUS_ATIVO);
        alerta.setAcaoSugerida("Priorizar operacao e revisar janela do berco.");
        alertaRepository.save(alerta);
    }

    private void atualizarAlertaAtraso(Alerta alerta, StatusNavio navio, int atraso) {
        alerta.setSeveridade(atraso >= 360 ? "critica" : atraso >= 120 ? "alta" : "media");
        alerta.setDescricao("Navio " + navio.getNomeNavio() + " atrasado em " + atraso + " minutos");
        alerta.setAcaoSugerida("Priorizar operacao e revisar janela do berco.");
        alertaRepository.save(alerta);
    }

    private void criarAlertaGargalo(CapacidadeYard zona, double percentual) {
        Alerta alerta = new Alerta();
        alerta.setTipo("GARGALO_YARD");
        alerta.setSeveridade(percentual >= 95d ? "critica" : "alta");
        alerta.setEntidadeId("YARD-" + zona.getZona());
        alerta.setDescricao("Zona " + zona.getZona() + " com ocupacao em " + round(percentual) + "%");
        alerta.setDataGerada(LocalDateTime.now());
        alerta.setStatus(STATUS_ATIVO);
        alerta.setAcaoSugerida("Bloquear novas entradas e priorizar saidas.");
        alertaRepository.save(alerta);
    }

    private void atualizarAlertaGargalo(Alerta alerta, CapacidadeYard zona, double percentual) {
        alerta.setSeveridade(percentual >= 95d ? "critica" : "alta");
        alerta.setDescricao("Zona " + zona.getZona() + " com ocupacao em " + round(percentual) + "%");
        alerta.setAcaoSugerida("Bloquear novas entradas e priorizar saidas.");
        alertaRepository.save(alerta);
    }

    private Alerta resolver(Alerta alerta, String usuario) {
        if (STATUS_RESOLVIDO.equalsIgnoreCase(alerta.getStatus())) {
            return alerta;
        }
        LocalDateTime agora = LocalDateTime.now();
        if (alerta.getDataReconhecimento() == null) {
            alerta.setDataReconhecimento(agora);
            alerta.setReconhecidoPor(normalizarUsuario(usuario));
        }
        alerta.setStatus(STATUS_RESOLVIDO);
        alerta.setDataResolucao(agora);
        alerta.setResolvidoPor(normalizarUsuario(usuario));
        return alertaRepository.save(alerta);
    }

    private List<String> normalizarLista(List<String> valores, boolean maiusculo) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream()
                .filter(StringUtils::hasText)
                .map(valor -> maiusculo ? valor.trim().toUpperCase(Locale.ROOT) : normalizar(valor))
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toLowerCase(Locale.ROOT) : "";
    }

    private String normalizarUsuario(String usuario) {
        String normalizado = StringUtils.hasText(usuario)
                ? usuario.replaceAll("[\\p{Cntrl}]", "").trim()
                : USUARIO_SISTEMA;
        if (!StringUtils.hasText(normalizado)) {
            return USUARIO_SISTEMA;
        }
        return normalizado.substring(0, Math.min(normalizado.length(), 120));
    }

    private double calcularPercentual(CapacidadeYard yard) {
        if (yard.getPercentualOcupacao() != null) {
            return yard.getPercentualOcupacao();
        }
        if (yard.getCapacidadeTotal() == null || yard.getCapacidadeTotal() <= 0 || yard.getOcupacaoAtual() == null) {
            return 0d;
        }
        return (yard.getOcupacaoAtual() * 100.0) / yard.getCapacidadeTotal();
    }

    private double round(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
