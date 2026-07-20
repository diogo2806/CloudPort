package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.NavioGranelDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PoraoNavioDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.SetorTanktopDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PoraoNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.SetorTanktop;
import br.com.cloudport.servicoyard.estivagembulk.repositorio.NavioGranelRepositorio;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NavioGranelConsultaServico {

    private static final Comparator<NavioGranel> ORDEM_NAVIO = Comparator
            .comparing(NavioGranel::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(NavioGranel::getVersaoPerfil, Comparator.nullsLast(Long::compareTo));

    private final NavioGranelRepositorio repositorio;

    public NavioGranelConsultaServico(NavioGranelRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<NavioGranelDto> listarPerfis() {
        return repositorio.findByIsTemplateFalseOrderByNomeAsc()
                .stream()
                .sorted(ORDEM_NAVIO)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NavioGranelDto> listarModelos() {
        return repositorio.findByIsTemplateTrue()
                .stream()
                .sorted(ORDEM_NAVIO)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private NavioGranelDto toDto(NavioGranel navio) {
        NavioGranelDto dto = new NavioGranelDto();
        dto.setId(navio.getId());
        dto.setNavioCadastroId(navio.getNavioCadastroId());
        dto.setVersaoPerfil(navio.getVersaoPerfil());
        dto.setVersaoNavioCanonico(navio.getVersaoNavioCanonico());
        dto.setImo(navio.getImo());
        dto.setNome(navio.getNome());
        dto.setClasse(navio.getClasse() == null ? null : navio.getClasse().name());
        dto.setLpp(navio.getLpp());
        dto.setBoca(navio.getBoca());
        dto.setCalado(navio.getCalado());
        dto.setDeslocamento(navio.getDeslocamento());
        dto.setGm(navio.getGm());
        dto.setTpc(navio.getTpc());
        dto.setLcb(navio.getLcb());
        dto.setKm(navio.getKm());
        dto.setMct1cm(navio.getMct1cm());
        dto.setCaladoMaximo(navio.getCaladoMaximo());
        dto.setTrimMaximo(navio.getTrimMaximo());
        dto.setBandaMaxima(navio.getBandaMaxima());
        dto.setGmMinimo(navio.getGmMinimo());
        dto.setPesoLeveToneladas(navio.getPesoLeveToneladas());
        dto.setLcgPesoLeve(navio.getLcgPesoLeve());
        dto.setTcgPesoLeve(navio.getTcgPesoLeve());
        dto.setVcgPesoLeve(navio.getVcgPesoLeve());
        dto.setPesoLastroToneladas(navio.getPesoLastroToneladas());
        dto.setLcgLastro(navio.getLcgLastro());
        dto.setTcgLastro(navio.getTcgLastro());
        dto.setVcgLastro(navio.getVcgLastro());
        dto.setBmMaxPermitido(navio.getBmMaxPermitido());
        dto.setSfMaxPermitido(navio.getSfMaxPermitido());
        dto.setVersaoDadosHidrostaticos(navio.getVersaoDadosHidrostaticos());
        dto.setVersaoDadosEstruturais(navio.getVersaoDadosEstruturais());
        dto.setPosicoesSecoes(navio.getPosicoesSecoes());
        dto.setPesoLeveSecoes(navio.getPesoLeveSecoes());
        dto.setEmpuxoSecoes(navio.getEmpuxoSecoes());
        dto.setLimitesSfSecoes(navio.getLimitesSfSecoes());
        dto.setLimitesBmSecoes(navio.getLimitesBmSecoes());
        dto.setTemplate(navio.isTemplate());
        dto.setPoroes(navio.getPoroes()
                .stream()
                .sorted(Comparator.comparingInt(PoraoNavio::getNumero))
                .map(this::toDto)
                .collect(Collectors.toList()));
        dto.setTotalPoroes(dto.getPoroes().size());
        return dto;
    }

    private PoraoNavioDto toDto(PoraoNavio porao) {
        PoraoNavioDto dto = new PoraoNavioDto();
        dto.setId(porao.getId());
        dto.setNumero(porao.getNumero());
        dto.setComprimento(porao.getComprimento());
        dto.setLargura(porao.getLargura());
        dto.setAlturaUtil(porao.getAlturaUtil());
        dto.setAreaUtil(porao.getAreaUtil());
        dto.setAnguloAntepara(porao.getAnguloAntepara());
        dto.setPosLongInicio(porao.getPosLongInicio());
        dto.setPosLongFim(porao.getPosLongFim());
        dto.setSetores(porao.getSetores()
                .stream()
                .sorted(Comparator.comparing(SetorTanktop::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(this::toDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private SetorTanktopDto toDto(SetorTanktop setor) {
        SetorTanktopDto dto = new SetorTanktopDto();
        dto.setId(setor.getId());
        dto.setNome(setor.getNome());
        dto.setCapacidadeTM2(setor.getCapacidadeTM2());
        dto.setAreaM2(setor.getAreaM2());
        dto.setPosLongInicio(setor.getPosLongInicio());
        dto.setPosLongFim(setor.getPosLongFim());
        dto.setPosTransInicio(setor.getPosTransInicio());
        dto.setPosTransFim(setor.getPosTransFim());
        return dto;
    }
}
