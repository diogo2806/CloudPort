package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.AnaliseEmpilhamentoDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.EstabilidadeEstrutural;
import br.com.cloudport.servicoyard.estivagembulk.dto.MaterialLashingDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.NavioGranelDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PlanoEstivaBulkDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PosicaoBobinaDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PosicionarBobinaRequisicaoDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PressaoTanktopDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.TacktopDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.ValidacaoPlanoBulkDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.ViolacaoEstivaDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.ClasseNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.MaterialLashingBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PoraoNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import br.com.cloudport.servicoyard.estivagembulk.modelo.ResultadoValidacaoSeguranca;
import br.com.cloudport.servicoyard.estivagembulk.modelo.SetorTanktop;
import br.com.cloudport.servicoyard.estivagembulk.modelo.StatusPlanoEstiva;
import br.com.cloudport.servicoyard.estivagembulk.modelo.TipoLashing;
import br.com.cloudport.servicoyard.estivagembulk.repositorio.NavioGranelRepositorio;
import br.com.cloudport.servicoyard.estivagembulk.repositorio.PlanoEstivaBulkRepositorio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanoEstivaBulkServico {

    private static final int MINIMO_LINHAS_DUNNAGE = 2;
    private static final int MINIMO_CALCOS = 2;

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
        invalidarValidacao(plano);
        planoRepositorio.save(plano);
        return bobina;
    }

    @Transactional
    public PosicaoBobinaDto posicionarBobina(Long planoId, PosicionarBobinaRequisicaoDto requisicao) {
        PlanoEstivaBulk plano = buscarPlano(planoId);
        BobinaManifesto bobina = localizarBobina(plano, requisicao.getBobinaId());
        PoraoNavio porao = localizarPorao(plano, requisicao.getPoraoId());
        SetorTanktop setor = localizarSetor(porao, requisicao.getSetorId());

        PosicaoBobina posicao = new PosicaoBobina();
        posicao.setPlano(plano);
        posicao.setBobina(bobina);
        posicao.setPorao(porao);
        posicao.setSetor(setor);
        posicao.setCamada(requisicao.getCamada());
        posicao.setPosicaoX(requisicao.getPosicaoX());
        posicao.setPosicaoY(requisicao.getPosicaoY());
        posicao.setAnguloInclinacao(requisicao.getAnguloInclinacao());
        posicao.setEspessuraDunnageMm(requisicao.getEspessuraDunnageMm());
        posicao.setQuantidadeLinhasDunnage(requisicao.getQuantidadeLinhasDunnage());
        posicao.setLarguraDunnageMm(requisicao.getLarguraDunnageMm());
        posicao.setComprimentoContatoDunnageMm(requisicao.getComprimentoContatoDunnageMm());
        posicao.setQuantidadeCalcos(requisicao.getQuantidadeCalcos());
        posicao.setEspacamentoFileirasMm(requisicao.getEspacamentoFileirasMm());
        posicao.setTipoLashing(requisicao.getTipoLashing());
        posicao.setForcaRequeridaLashingKn(requisicao.getForcaRequeridaLashingKn());
        posicao.setSequenciaDescarga(requisicao.getSequenciaDescarga());
        posicao.setReferenciaRegra(requisicao.getReferenciaRegra());
        posicao.setVersaoEspecificacao(requisicao.getVersaoEspecificacao());
        posicao.setResponsavelValidacao(requisicao.getResponsavelValidacao());
        posicao.setResultadoValidacao(ResultadoValidacaoSeguranca.PENDENTE);

        validarTanktopSeCompleto(posicao);
        plano.getPosicoes().add(posicao);
        adicionarMateriais(plano, posicao, requisicao.getMateriaisLashing());
        posicao.setCapacidadeLashingDisponivelKn(calcularCapacidadeLashing(plano, posicao));
        invalidarValidacao(plano);
        planoRepositorio.save(plano);
        return toPosicaoDto(plano, posicao);
    }

    @Transactional(readOnly = true)
    public PlanoEstivaBulkDto buscarPorId(Long planoId) {
        PlanoEstivaBulk plano = buscarPlano(planoId);
        ValidacaoPlanoBulkDto validacao = executarValidacaoCompleta(plano, false);
        return toDto(plano, validacao.getEstabilidade(), validacao);
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

    @Transactional(readOnly = true)
    public TacktopDto calcularTacktop(Long planoId) {
        return tacktopServico.validarSecuring(buscarPlano(planoId));
    }

    @Transactional
    public ValidacaoPlanoBulkDto validarPlanoCompleto(Long planoId) {
        PlanoEstivaBulk plano = buscarPlano(planoId);
        ValidacaoPlanoBulkDto validacao = executarValidacaoCompleta(plano, true);
        aplicarResultadoCalculo(plano, validacao.getEstabilidade());
        planoRepositorio.save(plano);
        return validacao;
    }

    @Transactional(noRollbackFor = ValidacaoPlanoBulkException.class)
    public PlanoEstivaBulkDto validarEAprovar(Long planoId) {
        PlanoEstivaBulk plano = buscarPlano(planoId);
        ValidacaoPlanoBulkDto validacao = executarValidacaoCompleta(plano, true);
        EstabilidadeEstrutural estabilidade = validacao.getEstabilidade();
        aplicarResultadoCalculo(plano, estabilidade);

        if (!validacao.isAprovado() || !estabilidade.isOperacional()) {
            plano.setStatus(StatusPlanoEstiva.RASCUNHO);
            limparSnapshotAprovacao(plano);
            planoRepositorio.save(plano);
            throw new ValidacaoPlanoBulkException(
                    "Plano reprovado pela validação completa de tank top, empilhamento, dunnage, calçamento, lashing, sequência de descarga ou estabilidade operacional",
                    validacao);
        }

        plano.setStatus(StatusPlanoEstiva.APROVADO);
        plano.setVersaoHidroAprovacao(estabilidade.getVersaoDadosHidrostaticos());
        plano.setVersaoEstruturalAprovacao(estabilidade.getVersaoDadosEstruturais());
        plano.setMemoriaCalculoAprovacao(estabilidade.getMemoriaCalculo());
        plano.setAprovadoEm(LocalDateTime.now());
        planoRepositorio.save(plano);
        return toDto(plano, estabilidade, validacao);
    }

    private ValidacaoPlanoBulkDto executarValidacaoCompleta(PlanoEstivaBulk plano,
            boolean persistirResultado) {
        LocalDateTime validadoEm = LocalDateTime.now();
        List<ViolacaoEstivaDto> violacoes = new ArrayList<>();
        validarManifestoEPosicoes(plano, violacoes);

        List<PressaoTanktopDto> analisesTanktop = tanktopServico.verificarTodosSetores(plano.getPosicoes());
        analisesTanktop.forEach(analise -> adicionarTodas(violacoes, analise.getViolacoes()));

        List<AnaliseEmpilhamentoDto> analisesEmpilhamento = new ArrayList<>();
        Set<Long> poroes = plano.getPosicoes().stream()
                .filter(posicao -> posicao.getPorao() != null && posicao.getPorao().getId() != null)
                .map(posicao -> posicao.getPorao().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Long poraoId : poroes) {
            AnaliseEmpilhamentoDto analise = empilhamentoServico.analisarEmpilhamento(plano, poraoId);
            analisesEmpilhamento.add(analise);
            adicionarTodas(violacoes, analise.getViolacoes());
        }

        TacktopDto securing = tacktopServico.validarSecuring(plano);
        adicionarTodas(violacoes, securing.getViolacoes());

        EstabilidadeEstrutural estabilidade = estabilidadeServico.calcular(plano);
        adicionarTodas(violacoes, estabilidade.getViolacoes());
        if (!estabilidade.isOperacional()) {
            violacoes.add(perigo(
                    "ESTABILIDADE_NAO_OPERACIONAL",
                    "A estabilidade não possui dados hidrostáticos e estruturais versionados suficientes para aprovação",
                    plano.getId()));
        } else if (!estabilidade.isAprovado()) {
            violacoes.add(perigo(
                    "ESTABILIDADE_REPROVADA",
                    "A estabilidade estrutural foi reprovada para o mesmo snapshot do plano",
                    plano.getId()));
        }

        String versaoEspecificacao = valorUnico(
                plano.getPosicoes(),
                PosicaoBobina::getVersaoEspecificacao,
                "VERSAO_ESPECIFICACAO_DIVERGENTE",
                "Todas as posições devem usar a mesma versão de especificação",
                plano.getId(),
                violacoes);
        String referenciaRegra = valorUnico(
                plano.getPosicoes(),
                PosicaoBobina::getReferenciaRegra,
                "REGRA_SEGURANCA_DIVERGENTE",
                "Todas as posições devem usar a mesma referência de regra",
                plano.getId(),
                violacoes);
        String responsavel = valorUnico(
                plano.getPosicoes(),
                PosicaoBobina::getResponsavelValidacao,
                "RESPONSAVEL_VALIDACAO_DIVERGENTE",
                "Todas as posições devem possuir o mesmo responsável pelo snapshot",
                plano.getId(),
                violacoes);

        boolean aprovado = violacoes.stream().noneMatch(this::isPerigo);
        ValidacaoPlanoBulkDto dto = new ValidacaoPlanoBulkDto();
        dto.setAprovado(aprovado);
        dto.setVersaoPlano(plano.getVersao() != null ? plano.getVersao() : 0L);
        dto.setVersaoEspecificacao(versaoEspecificacao);
        dto.setReferenciaRegra(referenciaRegra);
        dto.setResponsavelValidacao(responsavel);
        dto.setValidadoEm(validadoEm);
        dto.setAnalisesTanktop(analisesTanktop);
        dto.setAnalisesEmpilhamento(analisesEmpilhamento);
        dto.setAnaliseSecuring(securing);
        dto.setEstabilidade(estabilidade);
        dto.setViolacoes(violacoes);

        if (persistirResultado) {
            persistirResultadoValidacao(plano, dto);
        }
        return dto;
    }

    private void validarManifestoEPosicoes(PlanoEstivaBulk plano,
            List<ViolacaoEstivaDto> violacoes) {
        if (plano.getBobinas().isEmpty()) {
            violacoes.add(perigo(
                    "MANIFESTO_VAZIO", "O plano não possui bobinas manifestadas", plano.getId()));
        }
        if (plano.getPosicoes().isEmpty()) {
            violacoes.add(perigo(
                    "PLANO_SEM_POSICOES", "O plano não possui bobinas posicionadas", plano.getId()));
        }

        Set<Long> bobinasPosicionadas = new HashSet<>();
        Set<Integer> sequencias = new HashSet<>();
        for (PosicaoBobina posicao : plano.getPosicoes()) {
            Long referencia = posicao.getId();
            if (posicao.getBobina() == null || posicao.getBobina().getId() == null) {
                violacoes.add(perigo(
                        "BOBINA_POSICAO_AUSENTE", "A posição não possui bobina válida", referencia));
            } else if (!bobinasPosicionadas.add(posicao.getBobina().getId())) {
                violacoes.add(perigo(
                        "BOBINA_POSICIONADA_DUPLICADA",
                        "A mesma bobina foi posicionada mais de uma vez",
                        referencia));
            }
            if (posicao.getEspessuraDunnageMm() == null || posicao.getEspessuraDunnageMm() <= 0.0
                    || posicao.getQuantidadeLinhasDunnage() == null
                    || posicao.getQuantidadeLinhasDunnage() < MINIMO_LINHAS_DUNNAGE
                    || posicao.getLarguraDunnageMm() == null
                    || posicao.getLarguraDunnageMm() <= 0.0
                    || posicao.getComprimentoContatoDunnageMm() == null
                    || posicao.getComprimentoContatoDunnageMm() <= 0.0) {
                violacoes.add(perigo(
                        "DUNNAGE_NAO_COMPROVADO",
                        "Espessura, quantidade de linhas, largura e contato real do dunnage são obrigatórios",
                        referencia));
            }
            if (posicao.getQuantidadeCalcos() == null
                    || posicao.getQuantidadeCalcos() < MINIMO_CALCOS) {
                violacoes.add(perigo(
                        "CALCAMENTO_INSUFICIENTE",
                        "Cada posição deve comprovar ao menos dois calços",
                        referencia));
            }
            if (posicao.getSequenciaDescarga() == null || posicao.getSequenciaDescarga() <= 0) {
                violacoes.add(perigo(
                        "SEQUENCIA_DESCARGA_AUSENTE",
                        "A sequência de descarga deve ser positiva",
                        referencia));
            } else if (!sequencias.add(posicao.getSequenciaDescarga())) {
                violacoes.add(perigo(
                        "SEQUENCIA_DESCARGA_DUPLICADA",
                        "A sequência de descarga deve ser única no plano",
                        referencia));
            }
            if (vazio(posicao.getReferenciaRegra())
                    || vazio(posicao.getVersaoEspecificacao())
                    || vazio(posicao.getResponsavelValidacao())) {
                violacoes.add(perigo(
                        "RASTREABILIDADE_POSICAO_AUSENTE",
                        "Regra, versão da especificação e responsável são obrigatórios por posição",
                        referencia));
            }
        }

        for (BobinaManifesto bobina : plano.getBobinas()) {
            if (bobina.getId() != null && !bobinasPosicionadas.contains(bobina.getId())) {
                violacoes.add(perigo(
                        "BOBINA_NAO_POSICIONADA",
                        "A bobina " + bobina.getCodigo() + " ainda não foi posicionada",
                        bobina.getId()));
            }
            if (bobina.getPesoKg() == null || bobina.getPesoKg() <= 0.0
                    || bobina.getDiametroExternoMm() == null
                    || bobina.getDiametroExternoMm() <= 0.0
                    || bobina.getLarguraMm() == null
                    || bobina.getLarguraMm() <= 0.0
                    || vazio(bobina.getPortoDescarga())) {
                violacoes.add(perigo(
                        "DADOS_MANIFESTO_INCOMPLETOS",
                        "Peso, diâmetro, largura e porto de descarga reais são obrigatórios para a bobina "
                                + bobina.getCodigo(),
                        bobina.getId()));
            }
        }
    }

    private void persistirResultadoValidacao(PlanoEstivaBulk plano,
            ValidacaoPlanoBulkDto validacao) {
        ResultadoValidacaoSeguranca resultado = validacao.isAprovado()
                ? ResultadoValidacaoSeguranca.APROVADO
                : ResultadoValidacaoSeguranca.REPROVADO;
        plano.setVersaoValidacaoSeguranca(validacao.getVersaoPlano());
        plano.setVersaoEspecificacaoSeguranca(validacao.getVersaoEspecificacao());
        plano.setReferenciaRegraSeguranca(validacao.getReferenciaRegra());
        plano.setValidadoPorSeguranca(validacao.getResponsavelValidacao());
        plano.setValidadoEmSeguranca(validacao.getValidadoEm());
        plano.setResultadoValidacaoSeguranca(resultado);

        for (PosicaoBobina posicao : plano.getPosicoes()) {
            posicao.setResultadoValidacao(resultado);
            posicao.setValidadoEm(validacao.getValidadoEm());
            posicao.setCapacidadeLashingDisponivelKn(calcularCapacidadeLashing(plano, posicao));
        }
        for (MaterialLashingBulk material : plano.getMateriais()) {
            material.setResultadoValidacao(resultado);
            material.setValidadoEm(validacao.getValidadoEm());
        }
    }

    private void aplicarResultadoCalculo(PlanoEstivaBulk plano,
            EstabilidadeEstrutural estabilidade) {
        plano.setBmMaxCalculado(estabilidade.getBmMaxKnm());
        plano.setSfMaxCalculado(estabilidade.getSfMaxKn());
        plano.setTrimCalculado(estabilidade.getTrimMetros());
        plano.setListCalculado(estabilidade.getListGraus());
        plano.setGmCalculado(estabilidade.getGmMetros());
        plano.setCaladoSaida(estabilidade.getCaladoSaidaMetros());
    }

    private void validarTanktopSeCompleto(PosicaoBobina posicao) {
        boolean completo = posicao.getQuantidadeLinhasDunnage() != null
                && posicao.getLarguraDunnageMm() != null
                && posicao.getComprimentoContatoDunnageMm() != null;
        if (!completo) {
            return;
        }
        PressaoTanktopDto pressao = tanktopServico.calcularPressao(posicao);
        if (pressao.isExcedido()) {
            throw new IllegalStateException(
                    "Posicionamento bloqueado por tank top: "
                            + pressao.getViolacoes().get(0).getDescricao());
        }
        if (!pressao.getViolacoes().isEmpty()) {
            posicao.setAlertaTanktop(pressao.getViolacoes().get(0).getDescricao());
        }
    }

    private void adicionarMateriais(PlanoEstivaBulk plano, PosicaoBobina posicao,
            List<MaterialLashingDto> materiais) {
        if (materiais == null) {
            return;
        }
        for (MaterialLashingDto dto : materiais) {
            MaterialLashingBulk material = new MaterialLashingBulk();
            material.setPlano(plano);
            material.setPosicao(posicao);
            material.setTipo(converterTipo(dto.getTipo()));
            material.setQuantidade(dto.getQuantidade() != null ? dto.getQuantidade() : 0);
            material.setComprimentoM(dto.getComprimentoM());
            material.setPesoUnitarioKg(dto.getPesoUnitarioKg());
            material.setPontoAmarracao(dto.getPontoAmarracao());
            material.setCapacidadeNominalKn(dto.getCapacidadeNominalKn());
            material.setCargaTrabalhoSeguraKn(dto.getCargaTrabalhoSeguraKn());
            material.setCertificado(dto.getCertificado());
            material.setReferenciaRegra(dto.getReferenciaRegra());
            material.setVersaoEspecificacao(dto.getVersaoEspecificacao());
            material.setResponsavelValidacao(dto.getResponsavelValidacao());
            material.setResultadoValidacao(ResultadoValidacaoSeguranca.PENDENTE);
            material.setDescricao(dto.getDescricao());
            plano.getMateriais().add(material);
        }
    }

    private double calcularCapacidadeLashing(PlanoEstivaBulk plano, PosicaoBobina posicao) {
        return plano.getMateriais().stream()
                .filter(material -> material.getPosicao() == posicao
                        || material.getPosicao() != null
                        && material.getPosicao().getId() != null
                        && material.getPosicao().getId().equals(posicao.getId()))
                .filter(material -> material.getCargaTrabalhoSeguraKn() != null
                        && material.getCargaTrabalhoSeguraKn() > 0.0
                        && material.getQuantidade() > 0)
                .mapToDouble(material -> material.getQuantidade()
                        * material.getCargaTrabalhoSeguraKn())
                .sum();
    }

    private TipoLashing converterTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return null;
        }
        try {
            return TipoLashing.valueOf(tipo);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void invalidarValidacao(PlanoEstivaBulk plano) {
        plano.setStatus(StatusPlanoEstiva.RASCUNHO);
        limparSnapshotAprovacao(plano);
        plano.setResultadoValidacaoSeguranca(ResultadoValidacaoSeguranca.PENDENTE);
        plano.setVersaoValidacaoSeguranca(null);
        plano.setVersaoEspecificacaoSeguranca(null);
        plano.setReferenciaRegraSeguranca(null);
        plano.setValidadoEmSeguranca(null);
        plano.setValidadoPorSeguranca(null);
        for (PosicaoBobina posicao : plano.getPosicoes()) {
            posicao.setResultadoValidacao(ResultadoValidacaoSeguranca.PENDENTE);
            posicao.setValidadoEm(null);
        }
        for (MaterialLashingBulk material : plano.getMateriais()) {
            material.setResultadoValidacao(ResultadoValidacaoSeguranca.PENDENTE);
            material.setValidadoEm(null);
        }
    }

    private void limparSnapshotAprovacao(PlanoEstivaBulk plano) {
        plano.setVersaoHidroAprovacao(null);
        plano.setVersaoEstruturalAprovacao(null);
        plano.setMemoriaCalculoAprovacao(null);
        plano.setAprovadoEm(null);
    }

    private BobinaManifesto localizarBobina(PlanoEstivaBulk plano, Long bobinaId) {
        return plano.getBobinas().stream()
                .filter(bobina -> bobina.getId().equals(bobinaId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Bobina não encontrada: " + bobinaId));
    }

    private PoraoNavio localizarPorao(PlanoEstivaBulk plano, Long poraoId) {
        return plano.getNavio().getPoroes().stream()
                .filter(porao -> porao.getId().equals(poraoId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Porão não encontrado: " + poraoId));
    }

    private SetorTanktop localizarSetor(PoraoNavio porao, Long setorId) {
        return porao.getSetores().stream()
                .filter(setor -> setor.getId().equals(setorId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Setor não encontrado: " + setorId));
    }

    private PlanoEstivaBulk buscarPlano(Long planoId) {
        return planoRepositorio.findById(planoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "PlanoEstivaBulk não encontrado: " + planoId));
    }

    private PlanoEstivaBulkDto toDto(PlanoEstivaBulk plano,
            EstabilidadeEstrutural estabilidade, ValidacaoPlanoBulkDto validacao) {
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
                .mapToDouble(bobina -> bobina.getPesoKg() != null
                        ? bobina.getPesoKg() / 1000.0
                        : 0.0)
                .sum();
        dto.setPesoTotalToneladas(Math.round(pesoTotal * 10.0) / 10.0);
        dto.setPosicoes(plano.getPosicoes().stream()
                .map(posicao -> toPosicaoDto(plano, posicao))
                .collect(Collectors.toList()));
        dto.setEstabilidade(estabilidade);
        dto.setValidacaoSeguranca(validacao);
        dto.setViolacoes(validacao != null ? validacao.getViolacoes() : new ArrayList<>());
        return dto;
    }

    private PosicaoBobinaDto toPosicaoDto(PlanoEstivaBulk plano, PosicaoBobina posicao) {
        PosicaoBobinaDto dto = new PosicaoBobinaDto();
        dto.setId(posicao.getId());
        if (posicao.getBobina() != null) {
            dto.setBobinaId(posicao.getBobina().getId());
            dto.setCodigoBobina(posicao.getBobina().getCodigo());
            dto.setPesoKg(posicao.getBobina().getPesoKg());
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
        dto.setPosicaoX(posicao.getPosicaoX());
        dto.setPosicaoY(posicao.getPosicaoY());
        dto.setAnguloInclinacao(posicao.getAnguloInclinacao());
        dto.setEspessuraDunnageMm(posicao.getEspessuraDunnageMm());
        dto.setQuantidadeLinhasDunnage(posicao.getQuantidadeLinhasDunnage());
        dto.setLarguraDunnageMm(posicao.getLarguraDunnageMm());
        dto.setComprimentoContatoDunnageMm(posicao.getComprimentoContatoDunnageMm());
        dto.setQuantidadeCalcos(posicao.getQuantidadeCalcos());
        dto.setEspacamentoFileirasMm(posicao.getEspacamentoFileirasMm());
        dto.setTipoLashing(posicao.getTipoLashing() != null
                ? posicao.getTipoLashing().name()
                : null);
        dto.setForcaRequeridaLashingKn(posicao.getForcaRequeridaLashingKn());
        dto.setCapacidadeLashingDisponivelKn(posicao.getCapacidadeLashingDisponivelKn());
        dto.setSequenciaDescarga(posicao.getSequenciaDescarga());
        dto.setReferenciaRegra(posicao.getReferenciaRegra());
        dto.setVersaoEspecificacao(posicao.getVersaoEspecificacao());
        dto.setResponsavelValidacao(posicao.getResponsavelValidacao());
        dto.setResultadoValidacao(posicao.getResultadoValidacao() != null
                ? posicao.getResultadoValidacao().name()
                : null);
        dto.setAlertaTanktop(posicao.getAlertaTanktop());
        dto.setMateriaisLashing(plano.getMateriais().stream()
                .filter(material -> material.getPosicao() == posicao
                        || material.getPosicao() != null
                        && material.getPosicao().getId() != null
                        && material.getPosicao().getId().equals(posicao.getId()))
                .map(this::toMaterialDto)
                .toList());
        return dto;
    }

    private MaterialLashingDto toMaterialDto(MaterialLashingBulk material) {
        MaterialLashingDto dto = new MaterialLashingDto();
        dto.setId(material.getId());
        dto.setPosicaoId(material.getPosicao() != null ? material.getPosicao().getId() : null);
        dto.setTipo(material.getTipo() != null ? material.getTipo().name() : null);
        dto.setQuantidade(material.getQuantidade());
        dto.setComprimentoM(material.getComprimentoM());
        dto.setPesoUnitarioKg(material.getPesoUnitarioKg());
        dto.setPontoAmarracao(material.getPontoAmarracao());
        dto.setCapacidadeNominalKn(material.getCapacidadeNominalKn());
        dto.setCargaTrabalhoSeguraKn(material.getCargaTrabalhoSeguraKn());
        dto.setCertificado(material.getCertificado());
        dto.setReferenciaRegra(material.getReferenciaRegra());
        dto.setVersaoEspecificacao(material.getVersaoEspecificacao());
        dto.setResponsavelValidacao(material.getResponsavelValidacao());
        dto.setResultadoValidacao(material.getResultadoValidacao() != null
                ? material.getResultadoValidacao().name()
                : null);
        dto.setDescricao(material.getDescricao());
        return dto;
    }

    private String valorUnico(List<PosicaoBobina> posicoes,
            Function<PosicaoBobina, String> extrator, String tipoViolacao, String descricao,
            Long referenciaId, List<ViolacaoEstivaDto> violacoes) {
        Set<String> valores = posicoes.stream()
                .map(extrator)
                .filter(valor -> valor != null && !valor.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (valores.size() != 1
                || valores.size() < posicoes.size()
                && posicoes.stream().anyMatch(posicao -> vazio(extrator.apply(posicao)))) {
            violacoes.add(perigo(tipoViolacao, descricao, referenciaId));
        }
        return valores.size() == 1 ? valores.iterator().next() : null;
    }

    private void adicionarTodas(List<ViolacaoEstivaDto> destino,
            List<ViolacaoEstivaDto> origem) {
        if (origem != null) {
            destino.addAll(origem);
        }
    }

    private ViolacaoEstivaDto perigo(String tipo, String descricao, Long referenciaId) {
        return new ViolacaoEstivaDto(tipo, descricao, referenciaId, "PERIGO");
    }

    private boolean isPerigo(ViolacaoEstivaDto violacao) {
        return "PERIGO".equals(violacao.getSeveridade());
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
