package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.EstadoCargaContainer;
import br.com.cloudport.servicoyard.edi.modelo.PosicaoBay;
import br.com.cloudport.servicoyard.vesselplanner.dto.ViolacaoHardConstraintDto;
import br.com.cloudport.servicoyard.vesselplanner.modelo.ComprimentoConteiner;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.PerfilGeometriaNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotPerfilNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusPerfilGeometriaNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoSlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.PerfilGeometriaNavioRepositorio;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GeometriaNavioServico {

    private final PerfilGeometriaNavioRepositorio perfilRepositorio;

    public GeometriaNavioServico(PerfilGeometriaNavioRepositorio perfilRepositorio) {
        this.perfilRepositorio = perfilRepositorio;
    }

    public PerfilGeometriaNavio carregarPerfilAprovado(String codigoNavio) {
        if (codigoNavio == null || codigoNavio.isBlank()) {
            throw new IllegalStateException("Bay Plan sem código de navio para localizar o perfil geométrico");
        }
        PerfilGeometriaNavio perfil = perfilRepositorio
                .findFirstByCodigoNavioIgnoreCaseAndStatusOrderByVersaoPerfilDesc(
                        codigoNavio,
                        StatusPerfilGeometriaNavio.APROVADO)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Perfil geométrico aprovado não encontrado para o navio " + codigoNavio));
        validarPerfil(perfil);
        return perfil;
    }

    public void aplicarPerfil(EstivagemPlan plan, PerfilGeometriaNavio perfil) {
        validarPerfil(perfil);
        plan.setPerfilGeometriaId(perfil.getId());
        plan.setPerfilGeometriaVersao(perfil.getVersaoPerfil());
        plan.setCondicaoCarregamento(perfil.getCondicaoCarregamento());
        plan.setComprimentoLpp(perfil.getComprimentoLpp());
        plan.setBoca(perfil.getBoca());
        plan.setCalado(perfil.getCalado());
        plan.setDeslocamento(perfil.getDeslocamento());
        plan.setGm(perfil.getGm());
        plan.setTpc(perfil.getTpc());
        plan.setLcb(perfil.getLcb());

        for (SlotPerfilNavio origem : perfil.getSlots()) {
            SlotNavio destino = new SlotNavio();
            destino.setEstivagem(plan);
            destino.setBay(origem.getBay());
            destino.setRowBay(origem.getRowBay());
            destino.setTier(origem.getTier());
            destino.setTipoSlot(origem.getTipoSlot());
            destino.setCodigoHatchCover(origem.getCodigoHatchCover());
            destino.setSobreHatchCover(origem.isSobreHatchCover());
            destino.setRestrito(origem.isRestrito());
            destino.setMotivoRestricao(origem.getMotivoRestricao());
            destino.setTomadaReefer(origem.isTomadaReefer());
            destino.setAceita20Pes(origem.isAceita20Pes());
            destino.setAceita40Pes(origem.isAceita40Pes());
            destino.setAceita45Pes(origem.isAceita45Pes());
            destino.setMaxPesoKg(origem.getMaxPesoKg());
            destino.setMaxPesoPilhaKg(origem.getMaxPesoPilhaKg());
            destino.setStatusAlertas(origem.isRestrito() ? "RESTRITO" : "OK");
            plan.getSlots().add(destino);
        }
    }

    public void posicionarConteineresImportados(EstivagemPlan plan, List<BayPlanContainer> containers) {
        Map<String, SlotNavio> slotsPorPosicao = new HashMap<>();
        for (SlotNavio slot : plan.getSlots()) {
            slotsPorPosicao.put(chave(slot.getBay(), slot.getRowBay(), slot.getTier()), slot);
        }

        Set<String> posicoesRecebidas = new HashSet<>();
        for (BayPlanContainer container : containers) {
            if (container == null) {
                throw new IllegalStateException("Bay Plan contém registro de contêiner nulo");
            }
            PosicaoBay posicao = container.getPosicaoBay();
            if (posicao == null) {
                throw new IllegalStateException(
                        "Contêiner " + container.getCodigoContainer() + " sem posição BAPLIE");
            }
            String chavePosicao = chave(posicao.getBay(), posicao.getRow(), posicao.getTier());
            if (!posicoesRecebidas.add(chavePosicao)) {
                throw new IllegalStateException("Bay Plan contém posição duplicada: " + chavePosicao);
            }

            SlotNavio slot = slotsPorPosicao.get(chavePosicao);
            if (slot == null) {
                throw new IllegalStateException(
                        "Posição BAPLIE " + chavePosicao + " não existe no perfil geométrico aprovado");
            }

            Double pesoOperacional = container.getPesoOperacionalKg();
            List<ViolacaoHardConstraintDto> violacoes = verificarAlocacao(
                    plan,
                    slot,
                    container.getCodigoContainer(),
                    container.getIsoCode(),
                    pesoOperacional,
                    container.isReefer(),
                    container.isPerigoso(),
                    container.isOog());
            exigirSemPerigo(violacoes, container.getCodigoContainer(), chavePosicao);
            preencherSlot(slot, container);
        }
        validarLimitesPilha(plan);
    }

    public List<ViolacaoHardConstraintDto> verificarAlocacao(EstivagemPlan plan,
                                                              SlotNavio slot,
                                                              String codigoContainer,
                                                              String isoCode,
                                                              Double pesoKg,
                                                              boolean reefer,
                                                              boolean perigoso,
                                                              boolean oog) {
        List<ViolacaoHardConstraintDto> violacoes = new ArrayList<>();
        if (slot.isRestrito()
                || slot.getTipoSlot() == TipoSlotNavio.RESTRITO
                || slot.getTipoSlot() == TipoSlotNavio.ESCOTILHA) {
            violacoes.add(violacao(
                    "SLOT_RESTRITO",
                    "Slot restrito: " + textoRestricao(slot),
                    slot));
            return violacoes;
        }

        int comprimentoPes;
        try {
            comprimentoPes = ComprimentoConteiner.exigirComprimentoPes(isoCode);
        } catch (IllegalStateException exception) {
            violacoes.add(violacao("COMPRIMENTO_INVALIDO", exception.getMessage(), slot));
            return violacoes;
        }
        if (!slot.aceitaComprimentoPes(comprimentoPes)) {
            violacoes.add(violacao(
                    "COMPRIMENTO_INCOMPATIVEL",
                    "Slot não admite contêiner de " + comprimentoPes + " pés",
                    slot));
        }
        if (reefer && !slot.isTomadaReefer()) {
            violacoes.add(violacao(
                    "TOMADA_REEFER_AUSENTE",
                    "Slot não possui tomada reefer",
                    slot));
        }
        if (!tipoSlotCompativel(slot.getTipoSlot(), perigoso, reefer, oog)) {
            violacoes.add(violacao(
                    "TIPO_SLOT_INCOMPATIVEL",
                    "Tipo de slot incompatível com os atributos operacionais do contêiner",
                    slot));
        }
        if (perigoso && slot.isSobreHatchCover()) {
            violacoes.add(violacao(
                    "PERIGOSO_SOBRE_HATCH_COVER",
                    "Carga perigosa não pode ocupar este slot sobre hatch cover",
                    slot));
        }
        if (pesoKg == null || !Double.isFinite(pesoKg) || pesoKg <= 0.0) {
            violacoes.add(violacao(
                    "PESO_OPERACIONAL_INVALIDO",
                    "Peso operacional ausente ou inválido",
                    slot));
            return violacoes;
        }
        if (slot.getMaxPesoKg() == null || pesoKg > slot.getMaxPesoKg()) {
            violacoes.add(violacao(
                    "SOBREPESO_SLOT",
                    "Peso " + pesoKg + " kg excede o limite real do slot " + slot.getMaxPesoKg() + " kg",
                    slot));
        }

        double pesoPilha = plan.getSlots().stream()
                .filter(item -> item.getBay() == slot.getBay())
                .filter(item -> item.getRowBay() == slot.getRowBay())
                .filter(item -> item.getCodigoContainer() != null)
                .filter(item -> codigoContainer == null
                        || !codigoContainer.equalsIgnoreCase(item.getCodigoContainer()))
                .map(SlotNavio::getPesoKg)
                .filter(peso -> peso != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        if (slot.getMaxPesoPilhaKg() == null || pesoPilha + pesoKg > slot.getMaxPesoPilhaKg()) {
            violacoes.add(violacao(
                    "SOBREPESO_PILHA",
                    "Peso acumulado da pilha excede o limite real de "
                            + slot.getMaxPesoPilhaKg() + " kg",
                    slot));
        }
        return violacoes;
    }

    public void validarPlanoOperacional(EstivagemPlan plan) {
        if (plan.getPerfilGeometriaId() == null || plan.getPerfilGeometriaVersao() == null) {
            throw new IllegalStateException(
                    "Plano sem perfil geométrico versionado; operação bloqueada");
        }
        validarPositivo(plan.getComprimentoLpp(), "LPP do plano");
        validarPositivo(plan.getBoca(), "boca do plano");
        validarPositivo(plan.getCalado(), "calado do plano");
        validarPositivo(plan.getDeslocamento(), "deslocamento do plano");
        validarPositivo(plan.getGm(), "GM do plano");
        validarPositivo(plan.getTpc(), "TPC do plano");
        validarFinito(plan.getLcb(), "LCB do plano");
        if (plan.getSlots() == null || plan.getSlots().isEmpty()) {
            throw new IllegalStateException("Plano sem slots provenientes do perfil geométrico");
        }
    }

    public void validarPlanoParaAprovacao(EstivagemPlan plan) {
        validarPlanoOperacional(plan);
        for (SlotNavio slot : plan.getSlots()) {
            if (slot.getCodigoContainer() == null) {
                continue;
            }
            List<ViolacaoHardConstraintDto> violacoes = verificarAlocacao(
                    plan,
                    slot,
                    slot.getCodigoContainer(),
                    slot.getIsoCode(),
                    slot.getPesoKg(),
                    slot.isReefer(),
                    slot.isPerigoso(),
                    slot.isOog());
            exigirSemPerigo(violacoes, slot.getCodigoContainer(),
                    chave(slot.getBay(), slot.getRowBay(), slot.getTier()));
        }
        validarLimitesPilha(plan);
    }

    public boolean tipoSlotCompativel(TipoSlotNavio tipoSlot,
                                       boolean perigoso,
                                       boolean reefer,
                                       boolean oog) {
        if (tipoSlot == null || tipoSlot == TipoSlotNavio.ESCOTILHA || tipoSlot == TipoSlotNavio.RESTRITO) {
            return false;
        }
        if (oog) {
            return !perigoso && !reefer && tipoSlot == TipoSlotNavio.OOG;
        }
        if (perigoso && reefer) {
            return tipoSlot == TipoSlotNavio.REEFER_PERIGOSO;
        }
        if (perigoso) {
            return tipoSlot == TipoSlotNavio.PERIGOSO;
        }
        if (reefer) {
            return tipoSlot == TipoSlotNavio.REEFER;
        }
        return tipoSlot == TipoSlotNavio.NORMAL;
    }

    public void preencherSlot(SlotNavio slot, BayPlanContainer container) {
        slot.setCodigoContainer(container.getCodigoContainer());
        slot.setIsoCode(container.getIsoCode());
        slot.setPesoKg(container.getPesoOperacionalKg());
        slot.setPesoVgmKg(container.getPesoVgmKg());
        slot.setEstadoCarga(container.getEstadoCarga() != null
                ? container.getEstadoCarga()
                : EstadoCargaContainer.DESCONHECIDO);
        slot.setPortoCarga(container.getPortoCarga());
        slot.setPortoDescarga(container.getPortoDescarga());
        slot.setClasseImo(container.getClasseImo());
        slot.setNumeroOnu(container.getNumeroOnu());
        slot.setGrupoSegregacao(container.getGrupoSegregacao());
        slot.setPerigoso(container.isPerigoso());
        slot.setReefer(container.isReefer());
        slot.setTemperaturaRequeridaC(container.getTemperaturaRequeridaC());
        slot.setTemperaturaMinimaC(container.getTemperaturaMinimaC());
        slot.setTemperaturaMaximaC(container.getTemperaturaMaximaC());
        slot.setOog(container.isOog());
        slot.setExcessoFrontalCm(container.getExcessoFrontalCm());
        slot.setExcessoTraseiroCm(container.getExcessoTraseiroCm());
        slot.setExcessoEsquerdoCm(container.getExcessoEsquerdoCm());
        slot.setExcessoDireitoCm(container.getExcessoDireitoCm());
        slot.setExcessoAlturaCm(container.getExcessoAlturaCm());
        slot.setStatusAlertas("OK");
    }

    private void validarPerfil(PerfilGeometriaNavio perfil) {
        if (perfil.getId() == null || perfil.getVersaoPerfil() == null || perfil.getVersaoPerfil() <= 0) {
            throw new IllegalStateException("Perfil geométrico sem identidade ou versão válida");
        }
        if (perfil.getStatus() != StatusPerfilGeometriaNavio.APROVADO) {
            throw new IllegalStateException("Perfil geométrico não está aprovado");
        }
        if (perfil.getCondicaoCarregamento() == null || perfil.getCondicaoCarregamento().isBlank()) {
            throw new IllegalStateException("Perfil geométrico sem condição de carregamento");
        }
        validarPositivo(perfil.getComprimentoLpp(), "LPP do perfil");
        validarPositivo(perfil.getBoca(), "boca do perfil");
        validarPositivo(perfil.getCalado(), "calado do perfil");
        validarPositivo(perfil.getDeslocamento(), "deslocamento do perfil");
        validarPositivo(perfil.getGm(), "GM do perfil");
        validarPositivo(perfil.getTpc(), "TPC do perfil");
        validarFinito(perfil.getLcb(), "LCB do perfil");
        if (perfil.getLcb() < 0.0 || perfil.getLcb() > perfil.getComprimentoLpp()) {
            throw new IllegalStateException("LCB do perfil fora do comprimento do navio");
        }
        if (perfil.getSlots() == null || perfil.getSlots().isEmpty()) {
            throw new IllegalStateException("Perfil geométrico aprovado sem slots");
        }

        Set<String> posicoes = new HashSet<>();
        for (SlotPerfilNavio slot : perfil.getSlots()) {
            if (slot.getBay() <= 0 || slot.getRowBay() <= 0 || slot.getTier() <= 0) {
                throw new IllegalStateException("Perfil contém coordenada de slot inválida");
            }
            String chave = chave(slot.getBay(), slot.getRowBay(), slot.getTier());
            if (!posicoes.add(chave)) {
                throw new IllegalStateException("Perfil contém posição duplicada: " + chave);
            }
            if (slot.getTipoSlot() == null) {
                throw new IllegalStateException("Perfil contém slot sem tipo: " + chave);
            }
            validarPositivo(slot.getMaxPesoKg(), "limite de peso do slot " + chave);
            validarPositivo(slot.getMaxPesoPilhaKg(), "limite de pilha do slot " + chave);
            if (slot.getMaxPesoPilhaKg() < slot.getMaxPesoKg()) {
                throw new IllegalStateException("Limite de pilha menor que o limite do slot " + chave);
            }
            if (!slot.isRestrito()
                    && slot.getTipoSlot() != TipoSlotNavio.ESCOTILHA
                    && !slot.isAceita20Pes()
                    && !slot.isAceita40Pes()
                    && !slot.isAceita45Pes()) {
                throw new IllegalStateException("Slot operacional sem comprimento admissível: " + chave);
            }
            if ((slot.getTipoSlot() == TipoSlotNavio.REEFER
                    || slot.getTipoSlot() == TipoSlotNavio.REEFER_PERIGOSO)
                    && !slot.isTomadaReefer()) {
                throw new IllegalStateException("Slot reefer sem tomada configurada: " + chave);
            }
        }
    }

    private void validarLimitesPilha(EstivagemPlan plan) {
        Map<String, Double> pesos = new HashMap<>();
        Map<String, Double> limites = new HashMap<>();
        for (SlotNavio slot : plan.getSlots()) {
            String pilha = slot.getBay() + ":" + slot.getRowBay();
            if (slot.getMaxPesoPilhaKg() != null) {
                limites.merge(pilha, slot.getMaxPesoPilhaKg(), Math::min);
            }
            if (slot.getCodigoContainer() != null && slot.getPesoKg() != null) {
                pesos.merge(pilha, slot.getPesoKg(), Double::sum);
            }
        }
        for (Map.Entry<String, Double> entrada : pesos.entrySet()) {
            Double limite = limites.get(entrada.getKey());
            if (limite == null || entrada.getValue() > limite) {
                throw new IllegalStateException(
                        "Peso da pilha " + entrada.getKey() + " excede o limite real " + limite + " kg");
            }
        }
    }

    private void exigirSemPerigo(List<ViolacaoHardConstraintDto> violacoes,
                                  String codigoContainer,
                                  String posicao) {
        violacoes.stream()
                .filter(item -> "PERIGO".equals(item.getSeveridade()))
                .findFirst()
                .ifPresent(item -> {
                    throw new IllegalStateException(
                            "Contêiner " + codigoContainer + " incompatível com a posição "
                                    + posicao + ": " + item.getMensagem());
                });
    }

    private ViolacaoHardConstraintDto violacao(String codigo, String mensagem, SlotNavio slot) {
        return new ViolacaoHardConstraintDto(codigo, mensagem, slot.getId(), "PERIGO");
    }

    private String textoRestricao(SlotNavio slot) {
        return slot.getMotivoRestricao() == null || slot.getMotivoRestricao().isBlank()
                ? "posição indisponível no perfil"
                : slot.getMotivoRestricao();
    }

    private void validarPositivo(Double valor, String campo) {
        validarFinito(valor, campo);
        if (valor <= 0.0) {
            throw new IllegalStateException(campo + " deve ser maior que zero");
        }
    }

    private void validarFinito(Double valor, String campo) {
        if (valor == null || !Double.isFinite(valor)) {
            throw new IllegalStateException(campo + " ausente ou inválido");
        }
    }

    private String chave(int bay, int row, int tier) {
        return bay + ":" + row + ":" + tier;
    }
}
