package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.AnaliseEmpilhamentoDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.EstabilidadeEstrutural;
import br.com.cloudport.servicoyard.estivagembulk.dto.NavioGranelDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PlanoEstivaBulkDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PosicaoBobinaDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PosicionarBobinaRequisicaoDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PressaoTanktopDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.TacktopDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.ViolacaoEstivaDto;
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
            try { navio.setClasse(ClasseNavio.valueOf(dto.getClasse())); } catch (IllegalArgumentException ignored) {}
        }
        navio.setLpp(dto.getLpp());
        navio.setBoca(dto.getBoca());
        navio.setCalado(dto.getCalado());
        navio.setDeslocamento(dto.getDeslocamento());
        navio.setGm(dto.getGm() != null ? dto.getGm() : 1.5);
        navio.setBmMaxPermitido(dto.getBmMaxPermitido());
        navio.setSfMaxPermitido(dto.getSfMaxPermitido());
        navio.setTemplate(dto.isTemplate());
        return navioRepositorio.save(navio);
    }

    @Transactional
    public PlanoEstivaBulk criarPlano(Long navioId, String codigoViagem, String portoCarga, String portoDescarga) {
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
        planoRepositorio.save(plano);
        return bobina;
    }

    @Transactional
    public PosicaoBobinaDto posicionarBobina(Long planoId, PosicionarBobinaRequisicaoDto req) {
        PlanoEstivaBulk plano = buscarPlano(planoId);

        BobinaManifesto bobina = plano.getBobinas().stream()
                .filter(b -> b.getId().equals(req.getBobinaId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Bobina não encontrada: " + req.getBobinaId()));

        PoraoNavio porao = plano.getNavio().getPoroes().stream()
                .filter(p -> p.getId().equals(req.getPoraoId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Porão não encontrado: " + req.getPoraoId()));

        SetorTanktop setor = porao.getSetores().stream()
                .filter(s -> s.getId().equals(req.getSetorId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Setor não encontrado: " + req.getSetorId()));

        double dunnage = req.getEspessuraDunnageMm() > 0 ? req.getEspessuraDunnageMm() : 50.0;
        PressaoTanktopDto pressao = tanktopServico.calcularPressao(bobina, setor, dunnage);
        boolean bloqueado = pressao.getViolacoes().stream().anyMatch(v -> "PERIGO".equals(v.getSeveridade()));
        if (bloqueado) {
            throw new IllegalStateException("Posicionamento bloqueado por sobrecarga de tanktop: "
                    + pressao.getViolacoes().get(0).getDescricao());
        }

        PosicaoBobina pos = new PosicaoBobina();
        pos.setPlano(plano);
        pos.setBobina(bobina);
        pos.setPorao(porao);
        pos.setSetor(setor);
        pos.setCamada(req.getCamada());
        pos.setPosicaoX(req.getPosicaoX());
        pos.setPosicaoY(req.getPosicaoY());
        pos.setEspessuraDunnageMm(dunnage);
        pos.setTipoLashing(req.getTipoLashing() != null ? req.getTipoLashing() : TipoLashing.SEM_LASHING);
        if (!pressao.getViolacoes().isEmpty()) {
            pos.setAlertaTanktop(pressao.getViolacoes().get(0).getDescricao());
        }
        plano.getPosicoes().add(pos);
        planoRepositorio.save(plano);
        return toPosicaoDto(pos);
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
        planoRepositorio.save(plano);
        return dto;
    }

    @Transactional
    public PlanoEstivaBulkDto validarEAprovar(Long planoId) {
        PlanoEstivaBulk plano = buscarPlano(planoId);
        EstabilidadeEstrutural est = estabilidadeServico.calcular(plano);
        if (!est.isAprovado()) {
            throw new IllegalStateException(
                    "Plano possui violações de Hard Constraint e não pode ser aprovado");
        }
        plano.setStatus(StatusPlanoEstiva.APROVADO);
        plano.setBmMaxCalculado(est.getBmMaxKnm());
        plano.setSfMaxCalculado(est.getSfMaxKn());
        plano.setCalado_saida(est.getCaladoSaidaMetros());
        planoRepositorio.save(plano);
        return toDto(plano, est);
    }

    private PlanoEstivaBulk buscarPlano(Long planoId) {
        return planoRepositorio.findById(planoId)
                .orElseThrow(() -> new EntityNotFoundException("PlanoEstivaBulk não encontrado: " + planoId));
    }

    private PlanoEstivaBulkDto toDto(PlanoEstivaBulk plano, EstabilidadeEstrutural est) {
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
                .mapToDouble(b -> b.getPesoKg() != null ? b.getPesoKg() / 1000.0 : 0.0).sum();
        dto.setPesoTotalToneladas(Math.round(pesoTotal * 10.0) / 10.0);
        dto.setPosicoes(plano.getPosicoes().stream().map(this::toPosicaoDto).collect(Collectors.toList()));
        dto.setEstabilidade(est);
        dto.setViolacoes(est.getViolacoes() != null ? est.getViolacoes() : new ArrayList<>());
        return dto;
    }

    private PosicaoBobinaDto toPosicaoDto(PosicaoBobina p) {
        PosicaoBobinaDto dto = new PosicaoBobinaDto();
        dto.setId(p.getId());
        if (p.getBobina() != null) {
            dto.setBobinaId(p.getBobina().getId());
            dto.setCodigoBobina(p.getBobina().getCodigo());
            dto.setPesoKg(p.getBobina().getPesoKg() != null ? p.getBobina().getPesoKg() : 0.0);
        }
        if (p.getPorao() != null) {
            dto.setPoraoId(p.getPorao().getId());
            dto.setPoraoNumero(p.getPorao().getNumero());
        }
        if (p.getSetor() != null) {
            dto.setSetorId(p.getSetor().getId());
            dto.setSetorNome(p.getSetor().getNome());
        }
        dto.setCamada(p.getCamada());
        dto.setPosicaoX(p.getPosicaoX() != null ? p.getPosicaoX() : 0.0);
        dto.setPosicaoY(p.getPosicaoY() != null ? p.getPosicaoY() : 0.0);
        dto.setAnguloInclinacao(p.getAnguloInclinacao() != null ? p.getAnguloInclinacao() : 0.0);
        dto.setEspessuraDunnageMm(p.getEspessuraDunnageMm() != null ? p.getEspessuraDunnageMm() : 50.0);
        dto.setTipoLashing(p.getTipoLashing() != null ? p.getTipoLashing().name() : null);
        dto.setAlertaTanktop(p.getAlertaTanktop());
        return dto;
    }
}
