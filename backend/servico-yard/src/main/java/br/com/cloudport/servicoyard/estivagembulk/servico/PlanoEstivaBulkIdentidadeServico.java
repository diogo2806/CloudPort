package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.AnaliseEmpilhamentoDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.EstabilidadeEstrutural;
import br.com.cloudport.servicoyard.estivagembulk.dto.NavioGranelDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PlanoEstivaBulkDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PosicaoBobinaDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PosicionarBobinaRequisicaoDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PressaoTanktopDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.TacktopDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.ValidacaoPlanoBulkDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.repositorio.NavioGranelRepositorio;
import br.com.cloudport.servicoyard.estivagembulk.repositorio.PlanoEstivaBulkRepositorio;
import br.com.cloudport.servicoyard.integracao.navio.ContextoPlanejamentoNavio;
import br.com.cloudport.servicoyard.integracao.navio.IdentidadePlanejamentoNavioServico;
import br.com.cloudport.servicoyard.integracao.navio.NavioPlanejamento;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanoEstivaBulkIdentidadeServico {

    private final PlanoEstivaBulkServico servico;
    private final NavioGranelRepositorio navioRepositorio;
    private final PlanoEstivaBulkRepositorio planoRepositorio;
    private final IdentidadePlanejamentoNavioServico identidadeServico;

    public PlanoEstivaBulkIdentidadeServico(
            PlanoEstivaBulkServico servico,
            NavioGranelRepositorio navioRepositorio,
            PlanoEstivaBulkRepositorio planoRepositorio,
            IdentidadePlanejamentoNavioServico identidadeServico) {
        this.servico = servico;
        this.navioRepositorio = navioRepositorio;
        this.planoRepositorio = planoRepositorio;
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

        dto.setImo(canonico.codigoImo());
        dto.setNome(canonico.nome());
        NavioGranel navio = servico.registrarNavio(dto);
        navio.setNavioCadastroId(canonico.identificador());
        navio.setVersaoPerfil(proximaVersao);
        navio.setVersaoNavioCanonico(canonico.versao());
        return navioRepositorio.save(navio);
    }

    @Transactional
    public PlanoEstivaBulk criarPlano(
            Long navioId,
            Long visitaNavioId,
            String codigoViagem,
            String portoCarga,
            String portoDescarga) {
        NavioGranel navio = navioRepositorio.findById(navioId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Perfil estrutural de navio não encontrado: " + navioId));
        validarPerfilCanonico(navio);

        ContextoPlanejamentoNavio contexto = identidadeServico.resolverVisita(
                visitaNavioId,
                navio.getNavioCadastroId(),
                navio.getImo(),
                codigoViagem);
        if (!Objects.equals(navio.getVersaoNavioCanonico(), contexto.navio().versao())) {
            throw new IllegalStateException(
                    "O perfil estrutural foi criado para uma versão anterior do cadastro canônico. Registre uma nova versão do perfil.");
        }

        PlanoEstivaBulk plano = servico.criarPlano(
                navioId,
                contexto.codigoViagem(),
                portoCarga,
                portoDescarga);
        plano.setNavioCadastroId(contexto.navio().identificador());
        plano.setVisitaNavioId(contexto.visita().identificador());
        plano.setCodigoVisita(contexto.visita().codigoVisita());
        plano.setVersaoPerfilNavio(navio.getVersaoPerfil());
        plano.setVersaoNavioCanonico(contexto.navio().versao());
        plano.setVersaoVisita(contexto.visita().versao());
        return planoRepositorio.save(plano);
    }

    @Transactional(readOnly = true)
    public List<PlanoEstivaBulkDto> listarPlanos(Long navioId, Long visitaNavioId) {
        return planoRepositorio.findByNavioIdAndVisitaNavioIdOrderByCriadoEmDesc(navioId, visitaNavioId)
                .stream()
                .map(plano -> enriquecer(servico.buscarPorId(plano.getId()), plano))
                .collect(Collectors.toList());
    }

    @Transactional
    public BobinaManifesto adicionarBobina(Long planoId, BobinaManifesto bobina) {
        validarFonteCanonica(buscarPlano(planoId));
        return servico.adicionarBobina(planoId, bobina);
    }

    @Transactional
    public PosicaoBobinaDto posicionarBobina(Long planoId, PosicionarBobinaRequisicaoDto requisicao) {
        validarFonteCanonica(buscarPlano(planoId));
        return servico.posicionarBobina(planoId, requisicao);
    }

    @Transactional(readOnly = true)
    public PlanoEstivaBulkDto buscarPorId(Long planoId) {
        PlanoEstivaBulkDto dto = servico.buscarPorId(planoId);
        return enriquecer(dto, buscarPlano(planoId));
    }

    @Transactional(readOnly = true)
    public EstabilidadeEstrutural calcularEstabilidade(Long planoId) {
        validarFonteCanonica(buscarPlano(planoId));
        return servico.calcularEstabilidade(planoId);
    }

    @Transactional(readOnly = true)
    public List<PressaoTanktopDto> analisarTanktop(Long planoId) {
        validarFonteCanonica(buscarPlano(planoId));
        return servico.analisarTanktop(planoId);
    }

    @Transactional(readOnly = true)
    public AnaliseEmpilhamentoDto analisarEmpilhamento(Long planoId, Long poraoId) {
        validarFonteCanonica(buscarPlano(planoId));
        return servico.analisarEmpilhamento(planoId, poraoId);
    }

    @Transactional(readOnly = true)
    public TacktopDto calcularTacktop(Long planoId) {
        validarFonteCanonica(buscarPlano(planoId));
        return servico.calcularTacktop(planoId);
    }

    @Transactional
    public ValidacaoPlanoBulkDto validarPlanoCompleto(Long planoId) {
        validarFonteCanonica(buscarPlano(planoId));
        return servico.validarPlanoCompleto(planoId);
    }

    @Transactional(noRollbackFor = ValidacaoPlanoBulkException.class)
    public PlanoEstivaBulkDto validarEAprovar(Long planoId) {
        validarFonteCanonica(buscarPlano(planoId));
        return enriquecer(servico.validarEAprovar(planoId), buscarPlano(planoId));
    }

    private PlanoEstivaBulk buscarPlano(Long planoId) {
        return planoRepositorio.findById(planoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "PlanoEstivaBulk não encontrado: " + planoId));
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

    private void validarPerfilCanonico(NavioGranel navio) {
        if (navio.getNavioCadastroId() == null
                || navio.getVersaoPerfil() == null
                || navio.getVersaoNavioCanonico() == null) {
            throw new IllegalStateException(
                    "O perfil estrutural não está vinculado e versionado sob a identidade canônica do navio.");
        }
    }

    private PlanoEstivaBulkDto enriquecer(PlanoEstivaBulkDto dto, PlanoEstivaBulk plano) {
        dto.setNavioCadastroId(plano.getNavioCadastroId());
        dto.setVisitaNavioId(plano.getVisitaNavioId());
        dto.setCodigoVisita(plano.getCodigoVisita());
        dto.setVersaoPerfilNavio(plano.getVersaoPerfilNavio());
        dto.setVersaoNavioCanonico(plano.getVersaoNavioCanonico());
        dto.setVersaoVisita(plano.getVersaoVisita());
        return dto;
    }
}
