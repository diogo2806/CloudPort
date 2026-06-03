package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.ViolacaoEstivaDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.ItemCargoSiderurgico;
import br.com.cloudport.servicoyard.estivagembulk.modelo.TipoCargaSiderurgica;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RegrasCargaSiderurgicaServico {

    private static final int MAX_CAMADAS_BOBINA = 3;
    private static final int MAX_CAMADAS_CHAPA = 4;
    private static final int MAX_CAMADAS_SLAB = 2;
    private static final int MAX_CAMADAS_TARUGO = 5;
    private static final int MAX_CAMADAS_VERGALHAO = 6;
    private static final int MAX_CAMADAS_PERFIL = 3;
    private static final int MAX_CAMADAS_TUBO = 4;
    private static final double PESO_MAXIMO_SLAB_KG = 30000.0;

    public int maxCamadasPorTipo(TipoCargaSiderurgica tipo) {
        return switch (tipo) {
            case BOBINA_ACO -> MAX_CAMADAS_BOBINA;
            case CHAPA_ACO -> MAX_CAMADAS_CHAPA;
            case SLAB -> MAX_CAMADAS_SLAB;
            case TARUGO -> MAX_CAMADAS_TARUGO;
            case VERGALHAO -> MAX_CAMADAS_VERGALHAO;
            case PERFIL -> MAX_CAMADAS_PERFIL;
            case TUBO_ACO -> MAX_CAMADAS_TUBO;
        };
    }

    public boolean requerBerceiro(TipoCargaSiderurgica tipo) {
        return tipo == TipoCargaSiderurgica.BOBINA_ACO || tipo == TipoCargaSiderurgica.TUBO_ACO;
    }

    public boolean requerReforcoPiso(TipoCargaSiderurgica tipo) {
        return tipo == TipoCargaSiderurgica.SLAB;
    }

    public boolean permiteEmpilhamentoMisto(TipoCargaSiderurgica tipo) {
        return tipo == TipoCargaSiderurgica.VERGALHAO || tipo == TipoCargaSiderurgica.TARUGO;
    }

    public List<ViolacaoEstivaDto> validarItem(ItemCargoSiderurgico item) {
        List<ViolacaoEstivaDto> violacoes = new ArrayList<>();

        if (item.getTipoCarga() == null) {
            violacoes.add(new ViolacaoEstivaDto("TIPO_CARGA_AUSENTE",
                    "Item " + item.getCodigo() + " sem tipo de carga definido",
                    null, "PERIGO"));
            return violacoes;
        }

        if (item.getPesoKg() == null || item.getPesoKg() <= 0) {
            violacoes.add(new ViolacaoEstivaDto("PESO_INVALIDO",
                    "Item " + item.getCodigo() + " com peso inválido",
                    null, "PERIGO"));
        }

        if (item.getTipoCarga() == TipoCargaSiderurgica.SLAB
                && item.getPesoKg() != null && item.getPesoKg() > PESO_MAXIMO_SLAB_KG) {
            violacoes.add(new ViolacaoEstivaDto("SLAB_PESO_EXCEDIDO",
                    "Slab " + item.getCodigo() + " excede " + (PESO_MAXIMO_SLAB_KG / 1000) + "t por unidade",
                    null, "AVISO"));
        }

        if (item.getTipoCarga() == TipoCargaSiderurgica.BOBINA_ACO
                && (item.getDiametroExternoMm() == null || item.getDiametroExternoMm() <= 0)) {
            violacoes.add(new ViolacaoEstivaDto("DIAMETRO_AUSENTE",
                    "Bobina " + item.getCodigo() + " sem diâmetro externo",
                    null, "AVISO"));
        }

        if (item.getPortoDescarga() == null || item.getPortoDescarga().isBlank()) {
            violacoes.add(new ViolacaoEstivaDto("PORTO_DESCARGA_AUSENTE",
                    "Item " + item.getCodigo() + " sem porto de descarga definido",
                    null, "PERIGO"));
        }

        return violacoes;
    }

    public void aplicarPadroesParaTipo(ItemCargoSiderurgico item) {
        if (item.getTipoCarga() == null) return;
        item.setRequerBerceiro(requerBerceiro(item.getTipoCarga()));
        item.setRequerReforcoPiso(requerReforcoPiso(item.getTipoCarga()));
        item.setMaxCamadasEmpilhamento(maxCamadasPorTipo(item.getTipoCarga()));
    }
}
