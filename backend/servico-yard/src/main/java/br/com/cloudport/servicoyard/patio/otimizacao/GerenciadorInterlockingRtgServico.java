package br.com.cloudport.servicoyard.patio.otimizacao;

import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GerenciadorInterlockingRtgServico {

    private final EquipamentoPatioRepositorio equipamentoRepositorio;
    private final Map<String, DireitoDePassagemDto> mapaDireitos = new HashMap<>();

    public GerenciadorInterlockingRtgServico(EquipamentoPatioRepositorio equipamentoRepositorio) {
        this.equipamentoRepositorio = equipamentoRepositorio;
    }

    @Transactional(readOnly = true)
    public List<EquipamentoPatio> obterRtgsOperacionais() {
        return equipamentoRepositorio.findByTipoEquipamentoAndStatusOperacional(
                TipoEquipamento.RTG,
                StatusEquipamento.OPERACIONAL
        );
    }

    @Transactional
    public boolean requisitarDireitoDePassagem(String identificadorRtg, Integer fila) {
        List<EquipamentoPatio> rtgsOperacionais = obterRtgsOperacionais();
        EquipamentoPatio rtgSolicitante = rtgsOperacionais.stream()
                .filter(r -> r.getIdentificador().equals(identificadorRtg))
                .findFirst()
                .orElse(null);

        if (rtgSolicitante == null) {
            return false;
        }

        List<EquipamentoPatio> rtgsMessmoFila = rtgsOperacionais.stream()
                .filter(r -> r.getColuna().equals(fila) && !r.getIdentificador().equals(identificadorRtg))
                .toList();

        for (EquipamentoPatio rtgOcupante : rtgsMessmoFila) {
            if (!verificarConflitoDeMovimento(rtgSolicitante, rtgOcupante)) {
                registrarDireitoDePassagem(identificadorRtg, fila);
                return true;
            }
        }

        return false;
    }

    @Transactional
    public void liberarDireitoDePassagem(String identificadorRtg) {
        mapaDireitos.remove(identificadorRtg);
    }

    @Transactional(readOnly = true)
    public List<ConflitoDireitoDto> identificarConflitosDetecto() {
        List<ConflitoDireitoDto> conflitos = new ArrayList<>();
        List<EquipamentoPatio> rtgsOperacionais = obterRtgsOperacionais();

        for (int i = 0; i < rtgsOperacionais.size(); i++) {
            for (int j = i + 1; j < rtgsOperacionais.size(); j++) {
                EquipamentoPatio rtg1 = rtgsOperacionais.get(i);
                EquipamentoPatio rtg2 = rtgsOperacionais.get(j);

                if (verificarConflitoDeMovimento(rtg1, rtg2)) {
                    conflitos.add(new ConflitoDireitoDto(
                            rtg1.getIdentificador(),
                            rtg2.getIdentificador(),
                            rtg1.getColuna(),
                            "BLOQUEIO_COLUNA"
                    ));
                }
            }
        }

        return conflitos;
    }

    @Transactional(readOnly = true)
    public SequenciaOperacaoRtgDto obterSequenciaOtimizada(Integer filaSolicitada) {
        List<EquipamentoPatio> rtgsOperacionais = obterRtgsOperacionais();
        List<EquipamentoPatio> rtgsFila = rtgsOperacionais.stream()
                .filter(r -> r.getColuna().equals(filaSolicitada))
                .sorted((r1, r2) -> Integer.compare(r1.getLinha(), r2.getLinha()))
                .toList();

        List<String> sequencia = new ArrayList<>();
        for (EquipamentoPatio rtg : rtgsFila) {
            sequencia.add(rtg.getIdentificador());
        }

        return new SequenciaOperacaoRtgDto(filaSolicitada, sequencia, calcularTempoEspera(rtgsFila));
    }

    private boolean verificarConflitoDeMovimento(EquipamentoPatio rtg1, EquipamentoPatio rtg2) {
        if (!rtg1.getColuna().equals(rtg2.getColuna())) {
            return false;
        }

        int distancia = Math.abs(rtg1.getLinha() - rtg2.getLinha());
        return distancia < 5;
    }

    private void registrarDireitoDePassagem(String identificadorRtg, Integer fila) {
        DireitoDePassagemDto direito = new DireitoDePassagemDto(
                identificadorRtg,
                fila,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15)
        );
        mapaDireitos.put(identificadorRtg, direito);
    }

    private Integer calcularTempoEspera(List<EquipamentoPatio> rtgs) {
        return rtgs.size() * 2;
    }

    public static class DireitoDePassagemDto {
        private String identificadorRtg;
        private Integer fila;
        private LocalDateTime concessao;
        private LocalDateTime expiracao;

        public DireitoDePassagemDto(String identificadorRtg, Integer fila,
                                     LocalDateTime concessao, LocalDateTime expiracao) {
            this.identificadorRtg = identificadorRtg;
            this.fila = fila;
            this.concessao = concessao;
            this.expiracao = expiracao;
        }

        public String getIdentificadorRtg() { return identificadorRtg; }
        public Integer getFila() { return fila; }
        public LocalDateTime getConcessao() { return concessao; }
        public LocalDateTime getExpiracao() { return expiracao; }
        public boolean estaExpirado() {
            return LocalDateTime.now().isAfter(expiracao);
        }
    }

    public static class ConflitoDireitoDto {
        private String rtg1;
        private String rtg2;
        private Integer filaSolicitude;
        private String tipoConflito;

        public ConflitoDireitoDto(String rtg1, String rtg2, Integer filaSolicitude, String tipoConflito) {
            this.rtg1 = rtg1;
            this.rtg2 = rtg2;
            this.filaSolicitude = filaSolicitude;
            this.tipoConflito = tipoConflito;
        }

        public String getRtg1() { return rtg1; }
        public String getRtg2() { return rtg2; }
        public Integer getFilaSolicitude() { return filaSolicitude; }
        public String getTipoConflito() { return tipoConflito; }
    }

    public static class SequenciaOperacaoRtgDto {
        private Integer filaSolicitude;
        private List<String> sequenciaRtgs;
        private Integer tempoEsperaMinutos;

        public SequenciaOperacaoRtgDto(Integer filaSolicitude, List<String> sequenciaRtgs,
                                        Integer tempoEsperaMinutos) {
            this.filaSolicitude = filaSolicitude;
            this.sequenciaRtgs = sequenciaRtgs;
            this.tempoEsperaMinutos = tempoEsperaMinutos;
        }

        public Integer getFilaSolicitude() { return filaSolicitude; }
        public List<String> getSequenciaRtgs() { return sequenciaRtgs; }
        public Integer getTempoEsperaMinutos() { return tempoEsperaMinutos; }
    }
}
