package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.AnaliseEmpilhamentoDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.BobinaManifestoDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.EstabilidadeEstrutural;
import br.com.cloudport.servicoyard.estivagembulk.dto.NavioGranelDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PlanoEstivaBulkDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PoraoNavioDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PosicaoBobinaDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PosicionarBobinaRequisicaoDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PressaoTanktopDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.SetorTanktopDto;
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
import br.com.cloudport.servicoyard.integracao.navio.ContextoPlanejamentoNavio;
import br.com.cloudport.servicoyard.integracao.navio.IdentidadePlanejamentoNavioServico;
import br.com.cloudport.servicoyard.integracao.navio.NavioPlanejamento;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private final IdentidadePlanejamentoNavioServico identidadeServico;

    public PlanoEstivaBulkServico(NavioGranelRepositorio navioRepositorio,
                                   PlanoEstivaBulkRepositorio planoRepositorio,
                                   TanktopCalculadorServico tanktopServico,
                                   EstabilidadeEstruturalServico estabilidadeServico,
                                   EmpilhamentoBobinaServico empilhamentoServico,
                                   TacktopServico tacktopServico,
                                   IdentidadePlanejamentoNavioServico identidadeServico) {
        this.navioRepositorio = navioRepositorio;
        this.planoRepositorio = planoRepositorio;
        this.tanktopServico = tanktopServico;
        this.estabilidadeServico = estabilidadeServico;
        this.empilhamentoServico = empilhamentoServico;
        this.tacktopServico = tacktopServico;
        this.identidadeServico = identidadeServico;
    }

    @Transactional
    public NavioGranel registrarNavio(NavioGranelDto dto) {
        NavioPlanejamento canonico = identidadeServico.buscarNavioCanonico(dto.getNavioCadastroId());
        Long proximaVersao = navioRepositorio
                .findTopByNavioCadastroIdOrderByVersaoPerfilDesc(canonico.identificador())
                .map(NavioGranel::getVersaoPerfil)
                .filter(Objects::nonNull)
                .map(versao -> versao + 1L)
                .orElse(1L);

        NavioGranel navio = new NavioGranel();
        navio.setNavioCadastroId(canonico.identificador());
        navio.setVersaoPerfil(proximaVersao);
        navio.setVersaoNavioCanonico(canonico.versao());
        navio.setImo(canonico.codigoImo());
        navio.setNome(canonico.nome());
        if (dto.getClasse() != null) {
            try {
                navio.setClasse(ClasseNavio.valueOf(dto.getClasse()));
            } catch (IllegalArgumentException ignored) {
                // A validação completa do cadastro permanece no fluxo proprietário do domínio.
            }
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

    @Transactional(readOnly = true)
    public List<NavioGranelDto> listarNavios(boolean templates) {
        List<NavioGranel> navios = templates
                ? navioRepositorio.findByIsTemplateTrue()
                : navioRepositorio.findByIsTemplateFalseOrderByNomeAsc();
        return navios.stream().map(this::toNavioDto).collect(Collectors.toList());
    }

    @Transactional
    public PlanoEstivaBulk criarPlano(Long navioId,
                                      Long visitaNavioId,
                                      String codigoViagem,
                                      String portoCarga,
                                      String portoDescarga) {
        NavioGranel navio = navioRepositorio.findById(navioId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Perfil estrutural de navio não encontrado: " + navioId));
        if (navio.getNavioCadastroId() == null
                || navio.getVersaoPerfil() == null
                || navio.getVersaoNavioCanonico() == null) {
            throw new IllegalStateException(
                    "O perfil estrutural não está vinculado e versionado sob a identidade canônica do navio.");
        }

        ContextoPlanejamentoNavio contexto = identidadeServico.resolverVisita(
                visitaNavioId,
                navio.getNavioCadastroId(),
                navio.getImo(),
                codigoViagem);
        if (!Objects.equals(navio.getVersaoNavioCanonico(), contexto.navio().versao())) {
            throw new IllegalStateException(
                    "O perfil estrutural foi criado para uma versão anterior do cadastro canônico. Registre uma nova versão do perfil.");
        }

        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);
        plano.setNavioCadastroId(contexto.navio().identificador());
        plano.setVisitaNavioId(contexto.visita().identificador());
        plano.setCodigoVisita(contexto.visita().codigoVisita());
        plano.setVersaoPerfilNavio(navio.getVersaoPerfil());
        plano.setVersaoNavioCanonico(contexto.navio().versao());
        plano.setVersaoVisita(contexto.visita().versao());
        plano.setCodigoViagem(contexto.codigoViagem());
        plano.setPortoCarga(portoCarga);
        plano.setPortoDescarga(portoDescarga);
        return planoRepositorio.save(plano);
    }

    @Transactional(readOnly = true)
    public List<PlanoEstivaBulkDto> listarPlanos(Long navioId, String codigoViagem) {
        return planoRepositorio.findByNavioIdOrderByCriadoEmDesc(navioId).stream()
                .filter(plano -> codigoViagem == null || codigoViagem.isBlank()
                        || codigoViagem.equalsIgnoreCase(plano.getCodigoViagem()))
                .map(plano -> toDto(plano, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public BobinaManifesto adicionarBobina(Long planoId, BobinaManifesto bobina) {
        PlanoEstivaBulk plano = buscarPlano(planoId);
        validarFonteCanonica(plano);
        bobina.setPlano(plano);
        plano.getBobinas().add(bobina);
        planoRepositorio.save(plano);
        return bobina;
    }

    @Transactional
    public PosicaoBobinaDto posicionarBobina(Long planoId, PosicionarBobinaRequisicaoDto req) {
        PlanoEstivaBulk plano = buscarPlano(planoId);
        validarFonteCanonica(plano);

        BobinaManifesto bobina = plano.getBobinas().stream()
                .filter(item -> item.getId().equals(req.getBobinaId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Bobina não encontrada: " + req.getBobinaId()));

        PoraoNavio porao = plano.getNavio().getPoroes().stream()
                .filter(item -> item.getId().equals(req.getPoraoId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Porão não encontrado: " + req.getPoraoId()));

        SetorTanktop setor = porao.getSetores().stream()
                .filter(item -> item.getId().equals(req.getSetorId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Setor não encontrado: " + req.getSetorId()));

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
        posicao.setTipoLashing(req.getTipoLashing() != null ? req.getTipoLashing() : TipoLashing.SEM_LASHING);
        if (!pressao.getViolacoes().isEmpty()) {
            posicao.setAlertaTanktop(pressao.getViolacoes().get(0).getDescricao());
        }
        plano.getPosicoes().add(posicao);
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
        PlanoEstivaBulk plano = buscarPlano(planoId);
        validarFonteCanonica(plano);
        return estabilidadeServico.calcular(plano);
    }

    @Transactional(readOnly = true)
    public List<PressaoTanktopDto> analisarTanktop(Long planoId) {
        PlanoEstivaBulk plano = buscarPlano(planoId);
        validarFonteCanonica(plano);
        return tanktopServico.verificarTodosSetores(plano.getPosicoes());
    }

    @Transactional(readOnly = true)
    public AnaliseEmpilhamentoDto analisarEmpilhamento(Long planoId, Long poraoId) {
        PlanoEstivaBulk plano = buscarPlano(planoId);
        validarFonteCanonica(plano);
        return empilhamentoServico.analisarEmpilhamento(plano, poraoId);
    }

    @Transactional
    public TacktopDto calcularTacktop(Long planoId) {
        PlanoEstivaBulk plano = buscarPlano(planoId);
        validarFonteCanonica(plano);
        TacktopDto dto = tacktopServico.calcularTacktop(plano);
        planoRepositorio.save(plano);
        return dto;
    }

    @Transactional
    public PlanoEstivaBulkDto validarEAprovar(Long planoId) {
        PlanoEstivaBulk plano = buscarPlano(planoId);
        validarFonteCanonica(plano);
        EstabilidadeEstrutural estabilidade = estabilidadeServico.calcular(plano);
        if (!estabilidade.isAprovado()) {
            throw new IllegalStateException("Plano possui violações de Hard Constraint e não pode ser aprovado");
        }
        plano.setStatus(StatusPlanoEstiva.APROVADO);
        plano.setBmMaxCalculado(estabilidade.getBmMaxKnm());
        plano.setSfMaxCalculado(estabilidade.getSfMaxKn());
        plano.setCalado_saida(estabilidade.getCaladoSaidaMetros());
        planoRepositorio.save(plano);
        return toDto(plano, estabilidade);
    }

    private PlanoEstivaBulk buscarPlano(Long planoId) {
        return planoRepositorio.findById(planoId)
                .orElseThrow(() -> new EntityNotFoundException("PlanoEstivaBulk não encontrado: " + planoId));
    }

    private void validarFonteCanonica(PlanoEstivaBulk plano) {
        identidadeServico.validarFontePersistida(
                plano.getVisitaNavioId(),
                plano.getNavioCadastroId(),
                plano.getNavio() == null ? null : plano.getNavio().getImo(),
                plano.getCodigoViagem(),
                plano.getVersaoNavioCanonico(),
                plano.getVersaoVisita());
        if (plano.getNavio() == null
                || !Objects.equals(plano.getVersaoPerfilNavio(), plano.getNavio().getVersaoPerfil())) {
            throw new IllegalStateException(
                    "A versão do perfil estrutural vinculada ao plano não está disponível ou foi alterada.");
        }
    }

    private NavioGranelDto toNavioDto(NavioGranel navio) {
        NavioGranelDto dto = new NavioGranelDto();
        dto.setId(navio.getId());
        dto.setNavioCadastroId(navio.getNavioCadastroId());
        dto.setVersaoPerfil(navio.getVersaoPerfil());
        dto.setVersaoNavioCanonico(navio.getVersaoNavioCanonico());
        dto.setImo(navio.getImo());
        dto.setNome(navio.getNome());
        dto.setClasse(navio.getClasse() != null ? navio.getClasse().name() : null);
        dto.setLpp(navio.getLpp());
        dto.setBoca(navio.getBoca());
        dto.setCalado(navio.getCalado());
        dto.setDeslocamento(navio.getDeslocamento());
        dto.setGm(navio.getGm());
        dto.setBmMaxPermitido(navio.getBmMaxPermitido());
        dto.setSfMaxPermitido(navio.getSfMaxPermitido());
        dto.setTemplate(navio.isTemplate());
        dto.setTotalPoroes(navio.getPoroes().size());
        dto.setPoroes(navio.getPoroes().stream().map(this::toPoraoDto).collect(Collectors.toList()));
        return dto;
    }

    private PoraoNavioDto toPoraoDto(PoraoNavio porao) {
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
        dto.setSetores(porao.getSetores().stream().map(this::toSetorDto).collect(Collectors.toList()));
        return dto;
    }

    private SetorTanktopDto toSetorDto(SetorTanktop setor) {
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

    private PlanoEstivaBulkDto toDto(PlanoEstivaBulk plano, EstabilidadeEstrutural estabilidade) {
        PlanoEstivaBulkDto dto = new PlanoEstivaBulkDto();
        dto.setId(plano.getId());
        dto.setNavioCadastroId(plano.getNavioCadastroId());
        dto.setVisitaNavioId(plano.getVisitaNavioId());
        dto.setCodigoVisita(plano.getCodigoVisita());
        dto.setVersaoPerfilNavio(plano.getVersaoPerfilNavio());
        dto.setVersaoNavioCanonico(plano.getVersaoNavioCanonico());
        dto.setVersaoVisita(plano.getVersaoVisita());
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
        dto.setBobinas(plano.getBobinas().stream()
                .map(bobina -> toBobinaDto(plano, bobina))
                .collect(Collectors.toList()));
        dto.setPosicoes(plano.getPosicoes().stream().map(this::toPosicaoDto).collect(Collectors.toList()));
        dto.setEstabilidade(estabilidade);
        dto.setViolacoes(estabilidade != null && estabilidade.getViolacoes() != null
                ? estabilidade.getViolacoes()
                : new ArrayList<ViolacaoEstivaDto>());
        return dto;
    }

    private BobinaManifestoDto toBobinaDto(PlanoEstivaBulk plano, BobinaManifesto bobina) {
        BobinaManifestoDto dto = new BobinaManifestoDto();
        dto.setId(bobina.getId());
        dto.setCodigo(bobina.getCodigo());
        dto.setPesoKg(bobina.getPesoKg());
        dto.setDiametroExternoMm(bobina.getDiametroExternoMm());
        dto.setDiametroInternoMm(bobina.getDiametroInternoMm());
        dto.setLarguraMm(bobina.getLarguraMm());
        dto.setGrauAco(bobina.getGrauAco());
        dto.setPortoDescarga(bobina.getPortoDescarga());
        dto.setPosicionada(plano.getPosicoes().stream()
                .anyMatch(posicao -> posicao.getBobina() != null
                        && bobina.getId().equals(posicao.getBobina().getId())));
        return dto;
    }

    private PosicaoBobinaDto toPosicaoDto(PosicaoBobina posicao) {
        PosicaoBobinaDto dto = new PosicaoBobinaDto();
        dto.setId(posicao.getId());
        if (posicao.getBobina() != null) {
            dto.setBobinaId(posicao.getBobina().getId());
            dto.setCodigoBobina(posicao.getBobina().getCodigo());
            dto.setPesoKg(posicao.getBobina().getPesoKg() != null ? posicao.getBobina().getPesoKg() : 0.0);
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
        dto.setAnguloInclinacao(posicao.getAnguloInclinacao() != null ? posicao.getAnguloInclinacao() : 0.0);
        dto.setEspessuraDunnageMm(posicao.getEspessuraDunnageMm() != null
                ? posicao.getEspessuraDunnageMm()
                : 50.0);
        dto.setTipoLashing(posicao.getTipoLashing() != null ? posicao.getTipoLashing().name() : null);
        dto.setAlertaTanktop(posicao.getAlertaTanktop());
        return dto;
    }
}
