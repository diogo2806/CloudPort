package br.com.cloudport.serviconavio.estiva.servico;

import br.com.cloudport.serviconavio.comum.validacao.SanitizadorEntrada;
import br.com.cloudport.serviconavio.escala.entidade.Escala;
import br.com.cloudport.serviconavio.escala.repositorio.EscalaRepositorio;
import br.com.cloudport.serviconavio.estiva.dto.CriarAtribuicaoEstivaDTO;
import br.com.cloudport.serviconavio.estiva.dto.CriarPlanoEstivaDTO;
import br.com.cloudport.serviconavio.estiva.dto.CriarTernoDTO;
import br.com.cloudport.serviconavio.estiva.dto.PlanoEstivaDetalheDTO;
import br.com.cloudport.serviconavio.estiva.entidade.AtribuicaoEstiva;
import br.com.cloudport.serviconavio.estiva.entidade.PlanoEstiva;
import br.com.cloudport.serviconavio.estiva.entidade.StatusPlanoEstiva;
import br.com.cloudport.serviconavio.estiva.entidade.Terno;
import br.com.cloudport.serviconavio.estiva.entidade.TiersEstiva;
import br.com.cloudport.serviconavio.estiva.repositorio.AtribuicaoEstivaRepositorio;
import br.com.cloudport.serviconavio.estiva.repositorio.PlanoEstivaRepositorio;
import br.com.cloudport.serviconavio.estiva.repositorio.TernoRepositorio;
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
    private final TernoRepositorio ternoRepositorio;
    private final EscalaRepositorio escalaRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public PlanoEstivaServico(PlanoEstivaRepositorio planoEstivaRepositorio,
                              AtribuicaoEstivaRepositorio atribuicaoEstivaRepositorio,
                              TernoRepositorio ternoRepositorio,
                              EscalaRepositorio escalaRepositorio,
                              SanitizadorEntrada sanitizadorEntrada) {
        this.planoEstivaRepositorio = planoEstivaRepositorio;
        this.atribuicaoEstivaRepositorio = atribuicaoEstivaRepositorio;
        this.ternoRepositorio = ternoRepositorio;
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

        if (dto.getCamadasPorao() + dto.getCamadasConves() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O navio deve ter ao menos uma camada (porão ou convés).");
        }

        PlanoEstiva plano = new PlanoEstiva();
        plano.setEscala(escala);
        plano.setStatus(StatusPlanoEstiva.RASCUNHO);
        plano.setBaias(dto.getBaias());
        plano.setFileiras(dto.getFileiras());
        plano.setCamadasPorao(dto.getCamadasPorao());
        plano.setCamadasConves(dto.getCamadasConves());

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
            if (existente.getTipoOperacao() == dto.getTipoOperacao()
                    && existente.getBaia() == dto.getBaia()
                    && existente.getFileira() == dto.getFileira()
                    && existente.getCamada() == dto.getCamada()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        String.format(Locale.ROOT, "A célula %d-%d-%d já está ocupada para %s.",
                                dto.getBaia(), dto.getFileira(), dto.getCamada(), dto.getTipoOperacao()));
            }
        }

        AtribuicaoEstiva atribuicao = new AtribuicaoEstiva();
        atribuicao.setTipoOperacao(dto.getTipoOperacao());
        atribuicao.setCodigoConteiner(codigo);
        atribuicao.setTipoCarga(dto.getTipoCarga());
        atribuicao.setPesoToneladas(dto.getPesoToneladas());
        atribuicao.setBaia(dto.getBaia());
        atribuicao.setFileira(dto.getFileira());
        atribuicao.setCamada(dto.getCamada());
        atribuicao.setConves(TiersEstiva.convesDoTier(dto.getCamada()));
        atribuicao.setPosicaoPatioOrigem(tratarTextoOpcional(dto.getPosicaoPatioOrigem()));
        atribuicao.setPosicaoPatioDestino(tratarTextoOpcional(dto.getPosicaoPatioDestino()));
        atribuicao.setSequenciaEmbarque(dto.getSequenciaEmbarque());
        atribuicao.setEmbarcado(false);

        plano.adicionarAtribuicao(atribuicao);
        return PlanoEstivaDetalheDTO.deEntidade(planoEstivaRepositorio.save(plano));
    }

    @Transactional
    public PlanoEstivaDetalheDTO registrarOperacao(Long atribuicaoId) {
        AtribuicaoEstiva atribuicao = obterAtribuicao(atribuicaoId);
        PlanoEstiva plano = atribuicao.getPlano();

        if (!atribuicao.isEmbarcado()) {
            atribuicao.setEmbarcado(true);
            atribuicao.setEmbarcadoEm(LocalDateTime.now());
            atualizarStatusAposOperacao(plano);
        }

        return PlanoEstivaDetalheDTO.deEntidade(plano);
    }

    @Transactional
    public PlanoEstivaDetalheDTO removerAtribuicao(Long atribuicaoId) {
        AtribuicaoEstiva atribuicao = obterAtribuicao(atribuicaoId);
        if (atribuicao.isEmbarcado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível remover um contêiner que já foi embarcado ou descarregado.");
        }
        PlanoEstiva plano = atribuicao.getPlano();
        plano.removerAtribuicao(atribuicao);
        atribuicaoEstivaRepositorio.delete(atribuicao);
        return PlanoEstivaDetalheDTO.deEntidade(planoEstivaRepositorio.save(plano));
    }

    @Transactional
    public PlanoEstivaDetalheDTO adicionarTerno(Long escalaId, CriarTernoDTO dto) {
        PlanoEstiva plano = obterPlanoPorEscala(escalaId);

        if (dto.getBaiaInicial() > dto.getBaiaFinal()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A baia inicial do terno não pode ser maior que a baia final.");
        }
        if (dto.getBaiaFinal() > plano.getBaias()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O terno excede o número de baias do navio (%d).", plano.getBaias()));
        }

        String identificador = sanitizadorEntrada
                .limparTextoObrigatorio(dto.getIdentificador(), "identificador do terno")
                .toUpperCase(Locale.ROOT);

        for (Terno existente : plano.getTernos()) {
            if (existente.getIdentificador().equalsIgnoreCase(identificador)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        String.format(Locale.ROOT, "Já existe o terno %s neste plano.", identificador));
            }
            if (existente.sobrepoe(dto.getBaiaInicial(), dto.getBaiaFinal())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        String.format(Locale.ROOT, "As baias do terno %s se sobrepõem ao terno %s.",
                                identificador, existente.getIdentificador()));
            }
        }

        Terno terno = new Terno();
        terno.setIdentificador(identificador);
        terno.setSequencia(dto.getSequencia());
        terno.setBaiaInicial(dto.getBaiaInicial());
        terno.setBaiaFinal(dto.getBaiaFinal());

        plano.adicionarTerno(terno);
        return PlanoEstivaDetalheDTO.deEntidade(planoEstivaRepositorio.save(plano));
    }

    @Transactional
    public PlanoEstivaDetalheDTO removerTerno(Long ternoId) {
        Terno terno = ternoRepositorio.findById(ternoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Terno não encontrado."));
        PlanoEstiva plano = terno.getPlano();
        plano.removerTerno(terno);
        ternoRepositorio.delete(terno);
        return PlanoEstivaDetalheDTO.deEntidade(planoEstivaRepositorio.save(plano));
    }

    private void atualizarStatusAposOperacao(PlanoEstiva plano) {
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
        if (baia < 1 || baia > plano.getBaias() || fileira < 1 || fileira > plano.getFileiras()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT,
                            "A célula %d-%d-%d está fora dos limites do navio (%d baias, %d fileiras).",
                            baia, fileira, camada, plano.getBaias(), plano.getFileiras()));
        }
        if (!TiersEstiva.tierValido(camada, plano.getCamadasPorao(), plano.getCamadasConves())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT,
                            "Tier %d inválido. Use porão %s ou convés %s.",
                            camada,
                            TiersEstiva.tiersPorao(plano.getCamadasPorao()),
                            TiersEstiva.tiersConves(plano.getCamadasConves())));
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
