package br.com.cloudport.servicorail.ferrovia.importacao;

import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResultadoManifestoVisita {

    private String identificadorTrem;
    private String operadoraFerroviaria;
    private LocalDateTime horaChegadaPrevista;
    private LocalDateTime horaPartidaPrevista;
    private StatusVisitaTrem statusVisita = StatusVisitaTrem.PLANEJADO;
    private final List<String> identificacoesDescarga = new ArrayList<>();
    private final List<String> identificacoesCarga = new ArrayList<>();
    private final List<VagaoManifestoImportado> vagoes = new ArrayList<>();

    public String getIdentificadorTrem() {
        return identificadorTrem;
    }

    public void setIdentificadorTrem(String identificadorTrem) {
        this.identificadorTrem = identificadorTrem;
    }

    public String getOperadoraFerroviaria() {
        return operadoraFerroviaria;
    }

    public void setOperadoraFerroviaria(String operadoraFerroviaria) {
        this.operadoraFerroviaria = operadoraFerroviaria;
    }

    public LocalDateTime getHoraChegadaPrevista() {
        return horaChegadaPrevista;
    }

    public void setHoraChegadaPrevista(LocalDateTime horaChegadaPrevista) {
        this.horaChegadaPrevista = horaChegadaPrevista;
    }

    public LocalDateTime getHoraPartidaPrevista() {
        return horaPartidaPrevista;
    }

    public void setHoraPartidaPrevista(LocalDateTime horaPartidaPrevista) {
        this.horaPartidaPrevista = horaPartidaPrevista;
    }

    public StatusVisitaTrem getStatusVisita() {
        return statusVisita;
    }

    public void setStatusVisita(StatusVisitaTrem statusVisita) {
        this.statusVisita = statusVisita;
    }

    public List<String> getIdentificacoesDescarga() {
        return Collections.unmodifiableList(identificacoesDescarga);
    }

    public void adicionarConteinerDescarga(String identificacao) {
        if (identificacao != null) {
            identificacoesDescarga.add(identificacao);
        }
    }

    public List<String> getIdentificacoesCarga() {
        return Collections.unmodifiableList(identificacoesCarga);
    }

    public void adicionarConteinerCarga(String identificacao) {
        if (identificacao != null) {
            identificacoesCarga.add(identificacao);
        }
    }

    public List<VagaoManifestoImportado> getVagoes() {
        return Collections.unmodifiableList(vagoes);
    }

    public void adicionarVagao(Integer posicaoNoTrem, String identificadorVagao, String tipoVagao) {
        if (posicaoNoTrem != null && identificadorVagao != null) {
            vagoes.add(new VagaoManifestoImportado(posicaoNoTrem, identificadorVagao, tipoVagao));
        }
    }

    public static class VagaoManifestoImportado {
        private final Integer posicaoNoTrem;
        private final String identificadorVagao;
        private final String tipoVagao;

        public VagaoManifestoImportado(Integer posicaoNoTrem, String identificadorVagao, String tipoVagao) {
            this.posicaoNoTrem = posicaoNoTrem;
            this.identificadorVagao = identificadorVagao;
            this.tipoVagao = tipoVagao;
        }

        public Integer getPosicaoNoTrem() {
            return posicaoNoTrem;
        }

        public String getIdentificadorVagao() {
            return identificadorVagao;
        }

        public String getTipoVagao() {
            return tipoVagao;
        }
    }
}
