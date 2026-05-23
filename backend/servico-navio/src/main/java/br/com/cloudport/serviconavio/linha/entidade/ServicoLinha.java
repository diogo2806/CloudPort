package br.com.cloudport.serviconavio.linha.entidade;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

/**
 * Serviço de linha (carrier service) — a "linha" regular que um navio opera, com
 * uma rotação de portos (itinerário). Inspirado no conceito de Vessel Service do
 * Navis N4, onde visitas instanciam um serviço e sua rotação.
 */
@Entity
@Table(name = "servico_linha")
public class ServicoLinha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Long identificador;

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "armador", length = 80)
    private String armador;

    @OneToMany(mappedBy = "servicoLinha", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequencia ASC")
    private List<PortoRotacao> rotacao = new ArrayList<>();

    public void adicionarPorto(PortoRotacao porto) {
        porto.setServicoLinha(this);
        this.rotacao.add(porto);
    }

    public void limparRotacao() {
        this.rotacao.forEach(porto -> porto.setServicoLinha(null));
        this.rotacao.clear();
    }

    public Long getIdentificador() {
        return identificador;
    }

    public void setIdentificador(Long identificador) {
        this.identificador = identificador;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getArmador() {
        return armador;
    }

    public void setArmador(String armador) {
        this.armador = armador;
    }

    public List<PortoRotacao> getRotacao() {
        return rotacao;
    }

    public void setRotacao(List<PortoRotacao> rotacao) {
        this.rotacao = rotacao;
    }
}
