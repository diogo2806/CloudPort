package br.com.cloudport.serviconavio.estiva.servico;

import br.com.cloudport.serviconavio.comum.validacao.SanitizadorEntrada;
import br.com.cloudport.serviconavio.escala.entidade.Escala;
import br.com.cloudport.serviconavio.escala.repositorio.EscalaRepositorio;
import br.com.cloudport.serviconavio.estiva.dto.CriarAtribuicaoEstivaDTO;
import br.com.cloudport.serviconavio.estiva.dto.CriarPlanoEstivaDTO;
import br.com.cloudport.serviconavio.estiva.dto.EmbarqueDiretoGateResultadoDTO;
import br.com.cloudport.serviconavio.estiva.dto.PlanoEstivaDetalheDTO;
import br.com.cloudport.serviconavio.estiva.entidade.AtribuicaoEstiva;
import br.com.cloudport.serviconavio.estiva.entidade.PlanoEstiva;
import br.com.cloudport.serviconavio.estiva.entidade.StatusPlanoEstiva;
import br.com.cloudport.serviconavio.estiva.repositorio.AtribuicaoEstivaRepositorio;
import br.com.cloudport.serviconavio.estiva.repositorio.PlanoEstivaRepositorio;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlanoEstivaServico {

    private final PlanoEstivaRepositorio planoEstivaRepositorio;
    private final AtribuicaoEstivaRepositorio atribuicaoEstivaRepositorio;
    private final EscalaRepositorio escalaRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public PlanoEstivaServico(PlanoEstivaRepositorio planoEstivaRepositorio,
                               AtribuicaoEstivaRepositorio atribuicaoEstivaRepositorio,
                               EscalaRepositorio escalaRepositorio,
                               SanitizadorEntrada sanitizadorEntrada) {
        this.planoEstivaRepositorio = planoEstivaRepositorio;
        this.atribuicaoEstivaRepositorio = atribuicaoEstivaRepositorio;
        this.escalaRepositorio = escalaRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Transactional(readOnly = true)
    public PlanoEstivaDetalheDTO buscarPorEscala(Long escalaId) {
        return PlanoEstivaDetalheDTO.deEntidade(obterPlanoPorEscala(escalaId));
    }

    @Transactional
    public PlanoEstivaDetalheDTO criar(Long escalaId, CriarPlanoEstivaDTO dto) {
        Escala escala = escalaRepositorio.findById(escalaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escala não encontrada."));
        if (planoEstivaRepositorio.existsByEscalaId(escalaId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta escala já possui um plano de estiva.");
        }

        PlanoEstiva plano = new PlanoEstiva();
        plano.setEscala(escala);
        plano.setStatus(StatusPlanoEstiva.RASCUNHO);
        plano.setBaias(dto.getBaias());
        plano.setFileiras(dto.getFileiras());
        plano.setCamadas(dto.getCamadas());

        return PlanoEstivaDetalheDTO.deEntidade(planoEstivaRepositorio.save(plano));
    }

    @Transactional
    public PlanoEstivaDetalheDTO adicionarAtribuicao(Long escalaId, CriarAtribuicaoEstivaDTO dto) {
        PlanoEstiva plano = obterPlanoPorEscala(escalaId);
        if (plano.getStatus() == StatusPlanoEstiva.CONCLUIDO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O plano de estiva já foi concluído e não aceita novas atribuições.");
        }

        validarCelulaDentroDosLimites(plano, dto.getBaia(), dto.getFileira(), dto.getCamada());

        String codigo = sanitizadorEntrada
                .limparTextoObrigatorio(dto.getCodigoConteiner(), "código do contêiner")
                .toUpperCase(Locale.ROOT);

        for (AtribuicaoEstiva existente : plano.getAtribuicoes()) {
            if (existente.getCodigoConteiner().equalsIgnoreCase(codigo)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        String.format(Locale.ROOT, "O contêiner %s já está atribuído neste plano.", codigo));
            }
            if (existente.getBaia() == dto.getBaia()
                    && existente.getFileira() == dto.getFileira()
                    && existente.getCamada() == dto.getCamada()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        String.format(Locale.ROOT, "A célula %d-%d-%d já está ocupada.",
                                dto.getBaia(), dto.getFileira(), dto.getCamada()));
            }
        }

        AtribuicaoEstiva atribuicao = new AtribuicaoEstiva();
        atribuicao.setCodigoConteiner(codigo);
        atribuicao.setTipoCarga(dto.getTipoCarga());
        atribuicao.setPesoToneladas(dto.getPesoToneladas());
        atribuicao.setBaia(dto.getBaia());
        atribuicao.setFileira(dto.getFileira());
        atribuicao.setCamada(dto.getCamada());
        atribuicao.setPosicaoPatioOrigem(tratarTextoOpcional(dto.getPosicaoPatioOrigem()));
        atribuicao.setSequenciaEmbarque(dto.getSequenciaEmbarque());
        atribuicao.setEmbarcado(false);

        plano.adicionarAtribuicao(atribuicao);
        return PlanoEstivaDetalheDTO.deEntidade(planoEstivaRepositorio.save(plano));
    }

    @Transactional
    public PlanoEstivaDetalheDTO embarcar(Long atribuicaoId) {
        AtribuicaoEstiva atribuicao = obterAtribuicao(atribuicaoId);
        PlanoEstiva plano = atribuicao.getPlano();

        if (!atribuicao.isEmbarcado()) {
            atribuicao.setEmbarcado(true);
            atribuicao.setEmbarcadoEm(LocalDateTime.now());
            atualizarStatusAposEmbarque(plano);
        }

        return PlanoEstivaDetalheDTO.deEntidade(plano);
    }

    @Transactional
    public EmbarqueDiretoGateResultadoDTO embarcarDiretoDoGate(Long atribuicaoId,
                                                               String codigoConteiner,
                                                               LocalDateTime embarcadoEm) {
        AtribuicaoEstiva atribuicao = obterAtribuicao(atribuicaoId);
        String codigoNormalizado = sanitizadorEntrada
                .limparTextoObrigatorio(codigoConteiner, "código do contêiner")
                .toUpperCase(Locale.ROOT);

        if (!atribuicao.getCodigoConteiner().equalsIgnoreCase(codigoNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A atribuição de estiva pertence a outro contêiner.");
        }
        if (StringUtils.hasText(atribuicao.getPosicaoPatioOrigem())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A atribuição possui origem no pátio e deve seguir o fluxo normal pátio-navio.");
        }

        if (!atribuicao.isEmbarcado()) {
            atribuicao.setEmbarcado(true);
            atribuicao.setEmbarcadoEm(embarcadoEm != null ? embarcadoEm : LocalDateTime.now());
            atualizarStatusAposEmbarque(atribuicao.getPlano());
            atribuicaoEstivaRepositorio.save(atribuicao);
            planoEstivaRepositorio.save(atribuicao.getPlano());
        }

        return EmbarqueDiretoGateResultadoDTO.deEntidade(atribuicao);
    }

    @Transactional
    public PlanoEstivaDetalheDTO removerAtribuicao(Long atribuicaoId) {
        AtribuicaoEstiva atribuicao = obterAtribuicao(atribuicaoId);
        if (atribuicao.isEmbarcado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível remover um contêiner que já foi embarcado.");
        }
        PlanoEstiva plano = atribuicao.getPlano();
        plano.removerAtribuicao(atribuicao);
        atribuicaoEstivaRepositorio.delete(atribuicao);
        return PlanoEstivaDetalheDTO.deEntidade(planoEstivaRepositorio.save(plano));
    }

    private void atualizarStatusAposEmbarque(PlanoEstiva plano) {
        boolean todosEmbarcados = !plano.getAtribuicoes().isEmpty()
                && plano.getAtribuicoes().stream().allMatch(AtribuicaoEstiva::isEmbarcado);
        if (todosEmbarcados) {
            plano.setStatus(StatusPlanoEstiva.CONCLUIDO);
        } else if (plano.getStatus() == StatusPlanoEstiva.RASCUNHO
                || plano.getStatus() == StatusPlanoEstiva.CONFIRMADO) {
            plano.setStatus(StatusPlanoEstiva.EM_EXECUCAO);
        }
    }

    private void validarCelulaDentroDosLimites(PlanoEstiva plano, int baia, int fileira, int camada) {
        if (baia > plano.getBaias() || fileira > plano.getFileiras() || camada > plano.getCamadas()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT,
                            "A célula %d-%d-%d está fora dos limites do navio (%d baias, %d fileiras, %d camadas).",
                            baia, fileira, camada, plano.getBaias(), plano.getFileiras(), plano.getCamadas()));
        }
    }

    private PlanoEstiva obterPlanoPorEscala(Long escalaId) {
        return planoEstivaRepositorio.findByEscalaId(escalaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Plano de estiva não encontrado para esta escala."));
    }

    private AtribuicaoEstiva obterAtribuicao(Long atribuicaoId) {
        return atribuicaoEstivaRepositorio.findById(atribuicaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Atribuição de estiva não encontrada."));
    }

    private String tratarTextoOpcional(String valor) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        return StringUtils.hasText(limpo) ? limpo.toUpperCase(Locale.ROOT) : null;
    }
}
