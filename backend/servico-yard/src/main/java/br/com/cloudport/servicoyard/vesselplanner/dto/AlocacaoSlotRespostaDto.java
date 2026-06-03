package br.com.cloudport.servicoyard.vesselplanner.dto;

import java.util.List;

public class AlocacaoSlotRespostaDto {

    private boolean sucesso;
    private String mensagem;
    private EstabilidadeDto estabilidade;
    private List<ViolacaoHardConstraintDto> violacoesNovas;
    private SlotNavioDto slotAtualizado;

    public AlocacaoSlotRespostaDto() {
    }

    public static AlocacaoSlotRespostaDto ok(EstabilidadeDto e, SlotNavioDto s) {
        AlocacaoSlotRespostaDto dto = new AlocacaoSlotRespostaDto();
        dto.sucesso = true;
        dto.estabilidade = e;
        dto.slotAtualizado = s;
        return dto;
    }

    public static AlocacaoSlotRespostaDto falha(String msg, List<ViolacaoHardConstraintDto> v) {
        AlocacaoSlotRespostaDto dto = new AlocacaoSlotRespostaDto();
        dto.sucesso = false;
        dto.mensagem = msg;
        dto.violacoesNovas = v;
        return dto;
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public void setSucesso(boolean sucesso) {
        this.sucesso = sucesso;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public EstabilidadeDto getEstabilidade() {
        return estabilidade;
    }

    public void setEstabilidade(EstabilidadeDto estabilidade) {
        this.estabilidade = estabilidade;
    }

    public List<ViolacaoHardConstraintDto> getViolacoesNovas() {
        return violacoesNovas;
    }

    public void setViolacoesNovas(List<ViolacaoHardConstraintDto> violacoesNovas) {
        this.violacoesNovas = violacoesNovas;
    }

    public SlotNavioDto getSlotAtualizado() {
        return slotAtualizado;
    }

    public void setSlotAtualizado(SlotNavioDto slotAtualizado) {
        this.slotAtualizado = slotAtualizado;
    }
}
