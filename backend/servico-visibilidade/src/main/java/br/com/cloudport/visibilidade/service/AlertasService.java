package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.entity.Alerta;
import br.com.cloudport.visibilidade.entity.CapacidadeYard;
import br.com.cloudport.visibilidade.entity.StatusNavio;
import br.com.cloudport.visibilidade.repository.AlertaRepository;
import br.com.cloudport.visibilidade.repository.CapacidadeYardRepository;
import br.com.cloudport.visibilidade.repository.StatusNavioRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AlertasService {

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
        return alertaRepository.findByStatusOrderByDataGeradaDesc("ativo");
    }

    @Transactional(readOnly = true)
    public Page<Alerta> buscarAlertasFiltrados(List<String> severidades,
                                               List<String> tipos,
                                               String status,
                                               Pageable pageable) {
        return alertaRepository.findBySeveridadeInAndTipoInAndStatus(severidades, tipos, status, pageable);
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
        alerta.setStatus("ativo");
        alerta.setAcaoSugerida(acaoSugerida);
        return alertaRepository.save(alerta);
    }

    @Transactional
    public void resolverAlerta(Long id) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alerta nao encontrado: " + id));
        resolver(alerta);
    }

    @Transactional
    public void resolverAlertasAtivos(String entidadeId, String tipo) {
        if (!StringUtils.hasText(entidadeId) || !StringUtils.hasText(tipo)) {
            return;
        }

        alertaRepository.findByEntidadeIdAndStatus(entidadeId, "ativo").stream()
                .filter(alerta -> tipo.equalsIgnoreCase(alerta.getTipo()))
                .forEach(this::resolver);
    }

    @Transactional
    public void detectarAtrasos() {
        statusNavioRepository.findAll().forEach(navio -> {
            Integer atraso = navio.getAtrasoMinutos();
            if (atraso == null || atraso <= 30) {
                resolverAlertasAtivos(navio.getNavioId(), "ATRASO_NAVIO");
                return;
            }

            List<Alerta> alertasAtivos = alertaRepository.findByEntidadeIdAndStatus(navio.getNavioId(), "ativo");
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
            List<Alerta> alertasAtivos = alertaRepository.findByEntidadeIdAndStatus(entidadeId, "ativo");
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

    private void criarAlertaAtraso(StatusNavio navio, int atraso) {
        Alerta alerta = new Alerta();
        alerta.setTipo("ATRASO_NAVIO");
        alerta.setSeveridade(atraso >= 360 ? "critica" : atraso >= 120 ? "alta" : "media");
        alerta.setEntidadeId(navio.getNavioId());
        alerta.setDescricao("Navio " + navio.getNomeNavio() + " atrasado em " + atraso + " minutos");
        alerta.setDataGerada(LocalDateTime.now());
        alerta.setStatus("ativo");
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
        alerta.setStatus("ativo");
        alerta.setAcaoSugerida("Bloquear novas entradas e priorizar saidas.");
        alertaRepository.save(alerta);
    }

    private void atualizarAlertaGargalo(Alerta alerta, CapacidadeYard zona, double percentual) {
        alerta.setSeveridade(percentual >= 95d ? "critica" : "alta");
        alerta.setDescricao("Zona " + zona.getZona() + " com ocupacao em " + round(percentual) + "%");
        alerta.setAcaoSugerida("Bloquear novas entradas e priorizar saidas.");
        alertaRepository.save(alerta);
    }

    private void resolver(Alerta alerta) {
        alerta.setStatus("resolvido");
        alerta.setDataResolucao(LocalDateTime.now());
        alertaRepository.save(alerta);
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
