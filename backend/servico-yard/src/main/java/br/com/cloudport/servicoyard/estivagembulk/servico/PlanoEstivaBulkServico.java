package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.AnaliseEmpilhamentoDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.EstabilidadeEstrutural;
import br.com.cloudport.servicoyard.estivagembulk.dto.NavioGranelDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PlanoEstivaBulkDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PosicaoBobinaDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PosicionarBobinaRequisicaoDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PressaoTanktopDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.TacktopDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.ClasseNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PoraoNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import br.com.cloudport.servicoyard.estivagembulk.modelo.SetorTanktop;
import br.com.cloudport.servicoyard.estivagembulk.modelo.StatusPlanoEstiva;
import br.com.cloudport.servicoyard.estivagembulk.modelo.TipoLashing;
import br.com.cloudport.servicoyard.estivagembulk.repositorio.NavioGranelRepositorio;
import br.com.cloudport.servicoyard.estivagembulk.repositorio.PlanoEstivaBulkRepositorio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanoEstivaBulkServico {

    private final NavioGranelRepositorio navioRepositorio;
    private final PlanoEstivaBulkRepositorio planoRepositorio;
    private final TanktopCalculadorServico tanktopServico;
    private final EstabilidadeEstruturalServico estabilidadeServico;
    private final EmpilhamentoBobinaServico empilhamentoServico;
    private final TacktopServico tacktopServico;

    public PlanoEstivaBulkServico(NavioGranelRepositorio navioRepositorio,
                                   PlanoEstivaBulkRepositorio planoRepositorio,
                                   TanktopCalculadorServico tanktopServico,
                                   EstabilidadeEstruturalServico estabilidadeServico,
                                   EmpilhamentoBobinaServico empilhamentoServico,
                                   TacktopServico tacktopServico) {
        this.navioRepositorio = navioRepositorio;
        this.planoRepositorio = planoRepositorio;
        this.tanktopServico = tanktopServico;
        this.estabilidadeServico = estabilidadeServico;
        this.empilhamentoServico = empilhamentoServico;
        this.tacktopServico = tacktopServico;
    }

    @Transactional
    public NavioGranel registrarNavio(NavioGranelDto dto) {
        NavioGranel navio = new NavioGranel();
        navio.setImo(dto.getImo());
        navio.setNome(dto.getNome());
        if (dto.getClasse() != null) {
            try {
                navio.setClasse(ClasseNavio.valueOf(dto.getClasse()));
            } catch (IllegalArgumentException ignored) {
                navio.setClasse(null);
            }
        }
        navio.setLpp(dto.getLpp());
        navio.setBoca(dto.getBoca());
        navio.setCalado(dto.getCalado());
        navio.setDeslocamento(dto.getDeslocamento());
        navio.setGm(dto.getGm());
        navio.setTpc(dto.getTpc());
        navio.setLcb(dto.getLcb());
        navio.setKm(dto.getKm());
        navio.setMct1cm(dto.getMct1cm());
        navio.setCaladoMaximo(dto.getCaladoMaximo());
        navio.setTrimMaximo(dto.getTrimMaximo());
        navio.setBandaMaxima(dto.getBandaMaxima());
        navio.setGmMinimo(dto.getGmMinimo());
        navio.setPesoLeveToneladas(dto.getPesoLeveToneladas());
        navio.setLcgPesoLeve(dto.getLcgPesoLeve());
        navio.setTcgPesoLeve(dto.getTcgPesoLeve());
        navio.setVcgPesoLeve(dto.getVcgPesoLeve());
        navio.setPesoLastroToneladas(dto.getPesoLastroToneladas());
        navio.setLcgLastro(dto.getLcgLastro());
        navio.setTcgLastro(dto.getTcgLastro());
        navio.setVcgLastro(dto.getVcgLastro());
        navio.setBmMaxPermitido(dto.getBmMaxPermitido());
        navio.setSfMaxPermitido(dto.getSfMaxPermitido());
        navio.setVersaoDadosHidrostaticos(dto.getVersaoDadosHidrostaticos());
        navio.setVersaoDadosEstruturais(dto.getVersaoDadosEstruturais());
        navio.setPosicoesSecoes(dto.getPosicoesSecoes());
        navio.setPesoLeveSecoes(dto.getPesoLeveSecoes());
        navio.setEmpuxoSecoes(dto.getEmpuxoSecoes());
        navio.setLimitesSfSecoes(dto.getLimitesSfSecoes());
        navio.setLimitesBmSecoes(dto.getLimitesBmSecoes());
        navio.setTemplate(dto.isTemplate());
        return navioRepositorio.save(navio);
    }

    @Transactional
    public PlanoEstivaBulk criarPlano(Long navioId, String codigoViagem, String portoCarga,
            String portoDescarga) {
        NavioGranel navio = navioRepositorio.findById(navioId)
                .orElseThrow(() -> new EntityNotFoundException("Navio não encontrado: " + navioId));
        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);
        plano.setCodigoViagem(codigoViagem);
        plano.setPortoCarga(portoCarga);
        plano.setPortoDescarga(portoDescarga);
        return planoRepositorio.save(plano);
    }

    @Transactional
    public BobinaManifesto adicionarBobina(Long planoId, BobinaManifesto bobina) {
        PlanoEstivaBulk plano = buscarPlano(planoId);
        bobina.setPlano(plano);
        plano.getBobinas().add(bobina);
        invalidarAprovacao(plano);
        planoRepositorio.save(plano);
        return bobina;
    }

    @Transactional
    public PosicaoBobinaDto posicionarBobina(Long planoId, PosicionarBobinaRequisicaoDto req) {
        PlanoEstivaBulk plano = buscarPlano(planoId);

        BobinaManifesto bobina = plano.getBobinas().stream()
                .filter(item -> item.getId().equals(req.getBobinaId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Bobina não encontrada: " + req.getBobinaId()));

        PoraoNavio porao = plano.getNavio().getPoroes().stream()
                .filter(item -> item.getId().equals(req.getPoraoId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Porão não encontrado: " + req.getPoraoId()));

        SetorTanktop setor = porao.getSetores().stream()
                .filter(item -> item.getId().equals(req.getSetorId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Setor não encontrado: " + req.getSetorId()));

        double dunnage = req.getEspessuraDunnageMm() > 0 ? req.getEspessuraDunnageMm() : 50.0;
        PressaoTanktopDto pressao = tanktopServico.calcularPressao(bobina, setor, dunnage);
        boolean bloqueado = pressao.getViolacoes().stream()
                .anyMatch(violacao -> "PERIGO".equals(violacao.getSeveridade()));
        if (bloqueado) {
            throw new IllegalStateException("Posicionamento bloqueado por sobrecarga de tanktop: "
                    + pressao.getViolacoes().get(0).getDescricao());
        }

        PosicaoBobina posicao = new PosicaoBobina();
        posicao.setPlano(plano);
        posicao.setBobina(bobina);
        posicao.setPorao(porao);
        posicao.setSetor(setor);
        posicao.setCamada(req.getCamada());
        posicao.setPosicaoX(req.getPosicaoX());
        posicao.setPosicaoY(req.getPosicaoY());
        posicao.setEspessuraDunnageMm(dunnage);
        posicao.setTipoLashing(
                req.getTipoLashing() != null ? req.getTipoLashing() : TipoLashing.SEM_LASHING);
        if (!pressao.getViolacoes().isEmpty()) {
            posicao.setAlertaTanktop(pressao.getViolacoes().get(0).getDescricao());
        }
        plano.getPosicoes().add(posicao);
        invalidarAprovacao(plano);
        planoRepositorio.save(plano);
        return toPosicaoDto(posicao);
    }

    @Transactional(readOnly = true)
    public PlanoEstivaBulkDto buscarPorId(Long planoId) {
        PlanoEstivaBulk plano = buscarPlano(planoId);
        return toDto(plano, estabilidadeServico.calcular(plano));
    }

    @Transactional(readOnly = true)
    public EstabilidadeEstrutural calcularEstabilidade(Long planoId) {
        return estabilidadeServico.calcular(buscarPlano(planoId));
    }

    @Transactional(readOnly = true)
    public List<PressaoTanktopDto> analisarTanktop(Long planoId) {
        return tanktopServico.verificarTodosSetores(buscarPlano(planoId).getPosicoes());
    }

    @Transactional(readOnly = true)
    public AnaliseEmpilhamentoDto analisarEmpilhamento(Long planoId, Long poraoId) {
        return empilhamentoServico.analisarEmpilhamento(buscarPlano(planoId), poraoId);
    }

    @Transactional
    public TacktopDto calcularTacktop(Long planoId) {
        PlanoEstivaBulk plano = buscarPlano(planoId);
        TacktopDto dto = tacktopServico.calcularTacktop(plano);
        invalidarAprovacao(plano);
        planoRepositorio.save(plano);
        return dto;
    }

    @Transactional
    public PlanoEstivaBulkDto validarEAprovar(Long planoId) {
        PlanoEstivaBulk plano = buscarPlano(planoId);
        EstabilidadeEstrutural estabilidade = estabilidadeServico.calcular(plano);
        aplicarResultadoCalculo(plano, estabilidade);
        if (!estabilidade.isOperacional()) {
            throw new IllegalStateException(
                    "Plano não pode ser aprovado: cálculo hidroestrutural não operacional ou incompleto");
        }
        if (!estabilidade.isAprovado()) {
            throw new IllegalStateException(
                    "Plano possui violações de estabilidade ou resistência longitudinal e não pode ser aprovado");
        }
        plano.setStatus(StatusPlanoEstiva.APROVADO);
        plano.setVersaoHidroAprovacao(estabilidade.getVersaoDadosHidrostaticos());
        plano.setVersaoEstruturalAprovacao(estabilidade.getVersaoDadosEstruturais());
        plano.setMemoriaCalculoAprovacao(estabilidade.getMemoriaCalculo());
        plano.setAprovadoEm(LocalDateTime.now());
        planoRepositorio.save(plano);
        return toDto(plano, estabilidade);
    }

    private PlanoEstivaBulk buscarPlano(Long planoId) {
        return planoRepositorio.findById(planoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "PlanoEstivaBulk não encontrado: " + planoId));
    }

    private void aplicarResultadoCalculo(PlanoEstivaBulk plano,
            EstabilidadeEstrutural estabilidade) {
        plano.setBmMaxCalculado(estabilidade.getBmMaxKnm());
        plano.setSfMaxCalculado(estabilidade.getSfMaxKn());
        plano.setTrimCalculado(estabilidade.getTrimMetros());
        plano.setListCalculado(estabilidade.getListGraus());
        plano.setGmCalculado(estabilidade.getGmMetros());
        plano.setCalado_saida(estabilidade.getCaladoSaidaMetros());
    }

    private void invalidarAprovacao(PlanoEstivaBulk plano) {
        plano.setStatus(StatusPlanoEstiva.RASCUNHO);
        plano.setVersaoHidroAprovacao(null);
        plano.setVersaoEstruturalAprovacao(null);
        plano.setMemoriaCalculoAprovacao(null);
        plano.setAprovadoEm(null);
    }

    private PlanoEstivaBulkDto toDto(PlanoEstivaBulk plano, EstabilidadeEstrutural estabilidade) {
        PlanoEstivaBulkDto dto = new PlanoEstivaBulkDto();
        dto.setId(plano.getId());
        if (plano.getNavio() != null) {
            dto.setNavioId(plano.getNavio().getId());
            dto.setNomeNavio(plano.getNavio().getNome());
        }
        dto.setCodigoViagem(plano.getCodigoViagem());
        dto.setPortoCarga(plano.getPortoCarga());
        dto.setPortoDescarga(plano.getPortoDescarga());
        dto.setStatus(plano.getStatus() != null ? plano.getStatus().name() : null);
        dto.setTotalBobinas(plano.getBobinas().size());
        double pesoTotal = plano.getBobinas().stream()
                .mapToDouble(bobina -> bobina.getPesoKg() != null ? bobina.getPesoKg() / 1000.0 : 0.0)
                .sum();
        dto.setPesoTotalToneladas(Math.round(pesoTotal * 10.0) / 10.0);
        dto.setPosicoes(plano.getPosicoes().stream()
                .map(this::toPosicaoDto)
                .collect(Collectors.toList()));
        dto.setEstabilidade(estabilidade);
        dto.setViolacoes(estabilidade.getViolacoes() != null
                ? estabilidade.getViolacoes()
                : new ArrayList<>());
        return dto;
    }

    private PosicaoBobinaDto toPosicaoDto(PosicaoBobina posicao) {
        PosicaoBobinaDto dto = new PosicaoBobinaDto();
        dto.setId(posicao.getId());
        if (posicao.getBobina() != null) {
            dto.setBobinaId(posicao.getBobina().getId());
            dto.setCodigoBobina(posicao.getBobina().getCodigo());
            dto.setPesoKg(posicao.getBobina().getPesoKg() != null
                    ? posicao.getBobina().getPesoKg()
                    : 0.0);
        }
        if (posicao.getPorao() != null) {
            dto.setPoraoId(posicao.getPorao().getId());
            dto.setPoraoNumero(posicao.getPorao().getNumero());
        }
        if (posicao.getSetor() != null) {
            dto.setSetorId(posicao.getSetor().getId());
            dto.setSetorNome(posicao.getSetor().getNome());
        }
        dto.setCamada(posicao.getCamada());
        dto.setPosicaoX(posicao.getPosicaoX() != null ? posicao.getPosicaoX() : 0.0);
        dto.setPosicaoY(posicao.getPosicaoY() != null ? posicao.getPosicaoY() : 0.0);
        dto.setAnguloInclinacao(
                posicao.getAnguloInclinacao() != null ? posicao.getAnguloInclinacao() : 0.0);
        dto.setEspessuraDunnageMm(
                posicao.getEspessuraDunnageMm() != null ? posicao.getEspessuraDunnageMm() : 50.0);
        dto.setTipoLashing(
                posicao.getTipoLashing() != null ? posicao.getTipoLashing().name() : null);
        dto.setAlertaTanktop(posicao.getAlertaTanktop());
        return dto;
    }
}
