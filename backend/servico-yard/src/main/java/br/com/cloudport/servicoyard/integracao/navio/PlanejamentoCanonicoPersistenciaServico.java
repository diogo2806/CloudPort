package br.com.cloudport.servicoyard.integracao.navio;

import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.repositorio.BayPlanRepositorio;
import br.com.cloudport.servicoyard.estivagembulk.dto.PlanoEstivaBulkDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.repositorio.NavioGranelRepositorio;
import br.com.cloudport.servicoyard.estivagembulk.repositorio.PlanoEstivaBulkRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.dto.EstivagemPlanDto;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import java.util.Objects;
import javax.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanejamentoCanonicoPersistenciaServico {
    private final IdentidadePlanejamentoNavioServico identidadeServico;
    private final NavioGranelRepositorio navioRepositorio;
    private final PlanoEstivaBulkRepositorio planoBulkRepositorio;
    private final EstivagemPlanRepositorio estivagemPlanRepositorio;
    private final BayPlanRepositorio bayPlanRepositorio;

    public PlanejamentoCanonicoPersistenciaServico(
            IdentidadePlanejamentoNavioServico identidadeServico,
            NavioGranelRepositorio navioRepositorio,
            PlanoEstivaBulkRepositorio planoBulkRepositorio,
            EstivagemPlanRepositorio estivagemPlanRepositorio,
            BayPlanRepositorio bayPlanRepositorio) {
        this.identidadeServico = identidadeServico;
        this.navioRepositorio = navioRepositorio;
        this.planoBulkRepositorio = planoBulkRepositorio;
        this.estivagemPlanRepositorio = estivagemPlanRepositorio;
        this.bayPlanRepositorio = bayPlanRepositorio;
    }

    @Transactional
    public NavioGranel vincularPerfil(Long perfilId, Long navioCadastroId) {
        NavioGranel perfil = navioRepositorio.findById(perfilId)
                .orElseThrow(() -> new EntityNotFoundException("Perfil estrutural não encontrado: " + perfilId));
        NavioPlanejamento canonico = identidadeServico.buscarNavioCanonico(navioCadastroId);
        Long proximaVersao = navioRepositorio
                .findTopByNavioCadastroIdOrderByVersaoPerfilDesc(canonico.identificador())
                .map(NavioGranel::getVersaoPerfil)
                .filter(Objects::nonNull)
                .map(valor -> valor + 1L)
                .orElse(1L);
        perfil.setNavioCadastroId(canonico.identificador());
        perfil.setVersaoPerfil(proximaVersao);
        perfil.setVersaoNavioCanonico(canonico.versao());
        perfil.setImo(canonico.codigoImo());
        perfil.setNome(canonico.nome());
        return navioRepositorio.save(perfil);
    }

    @Transactional
    public PlanoEstivaBulk vincularPlanoBulk(Long planoId, Long visitaNavioId) {
        PlanoEstivaBulk plano = buscarPlanoBulk(planoId);
        NavioGranel perfil = plano.getNavio();
        validarPerfil(perfil);
        ContextoPlanejamentoNavio contexto = identidadeServico.resolverVisita(
                visitaNavioId, perfil.getNavioCadastroId(), perfil.getImo(), plano.getCodigoViagem());
        if (!Objects.equals(perfil.getVersaoNavioCanonico(), contexto.navio().versao())) {
            throw new IllegalStateException(
                    "O perfil estrutural foi criado para uma versão anterior do cadastro canônico.");
        }
        plano.setNavioCadastroId(contexto.navio().identificador());
        plano.setVisitaNavioId(contexto.visita().identificador());
        plano.setCodigoVisita(contexto.visita().codigoVisita());
        plano.setVersaoPerfilNavio(perfil.getVersaoPerfil());
        plano.setVersaoNavioCanonico(contexto.navio().versao());
        plano.setVersaoVisita(contexto.visita().versao());
        plano.setCodigoViagem(contexto.codigoViagem());
        return planoBulkRepositorio.save(plano);
    }

    @Transactional
    public EstivagemPlan vincularPlanoContainer(Long planoId, Long visitaNavioId) {
        EstivagemPlan plano = buscarPlanoContainer(planoId);
        BayPlan bayPlan = bayPlanRepositorio.findById(plano.getBayPlanId())
                .orElseThrow(() -> new EntityNotFoundException("BayPlan não encontrado: " + plano.getBayPlanId()));
        ContextoPlanejamentoNavio contexto = identidadeServico.resolverVisita(
                visitaNavioId, null, bayPlan.getCodigoNavio(), bayPlan.getCodigoViagem());
        plano.setNavioCadastroId(contexto.navio().identificador());
        plano.setVisitaNavioId(contexto.visita().identificador());
        plano.setCodigoVisita(contexto.visita().codigoVisita());
        plano.setVersaoNavioCanonico(contexto.navio().versao());
        plano.setVersaoVisita(contexto.visita().versao());
        plano.setCodigoNavio(contexto.navio().codigoImo());
        plano.setCodigoViagem(contexto.codigoViagem());
        return estivagemPlanRepositorio.save(plano);
    }

    @Transactional(readOnly = true)
    public void validarPlanoBulk(Long planoId) {
        PlanoEstivaBulk plano = buscarPlanoBulk(planoId);
        identidadeServico.validarFontePersistida(
                plano.getVisitaNavioId(), plano.getNavioCadastroId(),
                plano.getNavio() == null ? null : plano.getNavio().getImo(), plano.getCodigoViagem(),
                plano.getVersaoNavioCanonico(), plano.getVersaoVisita());
        if (plano.getNavio() == null
                || !Objects.equals(plano.getVersaoPerfilNavio(), plano.getNavio().getVersaoPerfil())) {
            throw new IllegalStateException("A versão do perfil estrutural vinculada ao plano foi alterada.");
        }
    }

    @Transactional(readOnly = true)
    public void validarPlanoContainer(Long planoId) {
        EstivagemPlan plano = buscarPlanoContainer(planoId);
        identidadeServico.validarFontePersistida(
                plano.getVisitaNavioId(), plano.getNavioCadastroId(), plano.getCodigoNavio(),
                plano.getCodigoViagem(), plano.getVersaoNavioCanonico(), plano.getVersaoVisita());
    }

    @Transactional(readOnly = true)
    public void enriquecer(PlanoEstivaBulkDto dto) {
        if (dto == null || dto.getId() == null) return;
        PlanoEstivaBulk plano = buscarPlanoBulk(dto.getId());
        dto.setNavioCadastroId(plano.getNavioCadastroId());
        dto.setVisitaNavioId(plano.getVisitaNavioId());
        dto.setCodigoVisita(plano.getCodigoVisita());
        dto.setVersaoPerfilNavio(plano.getVersaoPerfilNavio());
        dto.setVersaoNavioCanonico(plano.getVersaoNavioCanonico());
        dto.setVersaoVisita(plano.getVersaoVisita());
    }

    @Transactional(readOnly = true)
    public void enriquecer(EstivagemPlanDto dto) {
        if (dto == null || dto.getId() == null) return;
        EstivagemPlan plano = buscarPlanoContainer(dto.getId());
        dto.setNavioCadastroId(plano.getNavioCadastroId());
        dto.setVisitaNavioId(plano.getVisitaNavioId());
        dto.setCodigoVisita(plano.getCodigoVisita());
        dto.setVersaoNavioCanonico(plano.getVersaoNavioCanonico());
        dto.setVersaoVisita(plano.getVersaoVisita());
        dto.setCodigoNavio(plano.getCodigoNavio());
        dto.setCodigoViagem(plano.getCodigoViagem());
    }

    private PlanoEstivaBulk buscarPlanoBulk(Long id) {
        return planoBulkRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PlanoEstivaBulk não encontrado: " + id));
    }

    private EstivagemPlan buscarPlanoContainer(Long id) {
        return estivagemPlanRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("EstivagemPlan não encontrado: " + id));
    }

    private void validarPerfil(NavioGranel perfil) {
        if (perfil == null || perfil.getNavioCadastroId() == null
                || perfil.getVersaoPerfil() == null || perfil.getVersaoNavioCanonico() == null) {
            throw new IllegalStateException(
                    "O perfil estrutural não está vinculado e versionado sob a identidade canônica.");
        }
    }
}
