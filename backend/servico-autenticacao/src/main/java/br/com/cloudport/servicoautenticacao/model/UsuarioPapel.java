package br.com.cloudport.servicoautenticacao.model;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioPapel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Papel papel;

    @Enumerated(EnumType.STRING)
    private StatusUsuarioEnum status;

    public UsuarioPapel(Papel papel) {
        this.papel = papel;
        this.usuario = null;
    }

    public UsuarioPapel(Usuario usuario, Papel papel) {
        this.usuario = usuario;
        this.papel = papel;
    }
}
