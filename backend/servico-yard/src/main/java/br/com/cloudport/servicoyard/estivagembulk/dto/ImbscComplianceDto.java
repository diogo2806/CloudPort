package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.util.ArrayList;
import java.util.List;

public class ImbscComplianceDto {

    private boolean conforme;
    private String grupoImbsc;
    private boolean certificadoUmidadeRequerido;
    private boolean requerimentoAmarrioAtendido;
    private boolean segregacaoAtendida;
    private double fatorEstabilidadeGm;
    private List<ViolacaoEstivaDto> naoConformidades = new ArrayList<>();
    private List<String> documentosRequeridos = new ArrayList<>();

    public boolean isConforme() { return conforme; }
    public void setConforme(boolean conforme) { this.conforme = conforme; }

    public String getGrupoImbsc() { return grupoImbsc; }
    public void setGrupoImbsc(String grupoImbsc) { this.grupoImbsc = grupoImbsc; }

    public boolean isCertificadoUmidadeRequerido() { return certificadoUmidadeRequerido; }
    public void setCertificadoUmidadeRequerido(boolean certificadoUmidadeRequerido) { this.certificadoUmidadeRequerido = certificadoUmidadeRequerido; }

    public boolean isRequerimentoAmarrioAtendido() { return requerimentoAmarrioAtendido; }
    public void setRequerimentoAmarrioAtendido(boolean requerimentoAmarrioAtendido) { this.requerimentoAmarrioAtendido = requerimentoAmarrioAtendido; }

    public boolean isSegregacaoAtendida() { return segregacaoAtendida; }
    public void setSegregacaoAtendida(boolean segregacaoAtendida) { this.segregacaoAtendida = segregacaoAtendida; }

    public double getFatorEstabilidadeGm() { return fatorEstabilidadeGm; }
    public void setFatorEstabilidadeGm(double fatorEstabilidadeGm) { this.fatorEstabilidadeGm = fatorEstabilidadeGm; }

    public List<ViolacaoEstivaDto> getNaoConformidades() { return naoConformidades; }
    public void setNaoConformidades(List<ViolacaoEstivaDto> naoConformidades) { this.naoConformidades = naoConformidades; }

    public List<String> getDocumentosRequeridos() { return documentosRequeridos; }
    public void setDocumentosRequeridos(List<String> documentosRequeridos) { this.documentosRequeridos = documentosRequeridos; }
}
