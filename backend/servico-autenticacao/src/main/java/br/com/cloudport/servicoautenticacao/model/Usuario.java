package br.com.cloudport.servicoautenticacao.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Table(name = "users")
@Entity(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Usuario implements UserDetails {

    @Id
    private UUID id;

    @Column
    private String login;

    @Column(name = "password")
    private String senha;

    @Column(name = "nome")
    private String nome;

    @Column(name = "transportadora_documento")
    private String transportadoraDocumento;

    @Column(name = "transportadora_nome")
    private String transportadoraNome;

    @OneToMany(mappedBy = "usuario", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<UsuarioPapel> papeis = new HashSet<>();

    public Usuario(String login, String senha, Set<UsuarioPapel> papeis) {
        this.id = UUID.randomUUID();
        this.login = login;
        this.senha = senha;
        this.papeis = papeis != null ? papeis : new HashSet<>();
    }

    public Usuario(String login, String senha, String nome, String transportadoraDocumento,
                    String transportadoraNome, Set<UsuarioPapel> papeis) {
        this(login, senha, papeis);
        this.nome = nome;
        this.transportadoraDocumento = transportadoraDocumento;
        this.transportadoraNome = transportadoraNome;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.papeis.stream()
                .map(UsuarioPapel::getPapel)
                .filter(Objects::nonNull)
                .map(Papel::getNome)
                .filter(Objects::nonNull)
                .map(nomePapel -> nomePapel.startsWith("ROLE_") ? nomePapel : "ROLE_" + nomePapel.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getPassword() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public Set<UsuarioPapel> getPapeis() {
        return papeis;
    }

    public void setPapeis(Set<UsuarioPapel> papeis) {
        this.papeis = papeis;
    }
}
