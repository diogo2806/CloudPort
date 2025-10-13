package br.com.cloudport.servicoautenticacao.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Table(name = "users")
@Entity(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class User implements UserDetails {
    @Id
    private UUID id;

    @Column
    private String login;

    @Column
    private String password;

    @Column(name = "nome")
    private String nome;

    @Column(name = "transportadora_documento")
    private String transportadoraDocumento;

    @Column(name = "transportadora_nome")
    private String transportadoraNome;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<UserRole> roles;

    public User(String login, String password, Set<UserRole> roles){
        this.id = UUID.randomUUID();
        this.login = login;
        this.password = password;
        this.roles = roles;
    }

    public User(String login, String password, String nome, String transportadoraDocumento,
                String transportadoraNome, Set<UserRole> roles) {
        this(login, password, roles);
        this.nome = nome;
        this.transportadoraDocumento = transportadoraDocumento;
        this.transportadoraNome = transportadoraNome;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .map(Role::getName)
                .filter(Objects::nonNull)
                .map(name -> name.startsWith("ROLE_") ? name : "ROLE_" + name.toUpperCase())
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
        return password;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTransportadoraDocumento() {
        return transportadoraDocumento;
    }

    public void setTransportadoraDocumento(String transportadoraDocumento) {
        this.transportadoraDocumento = transportadoraDocumento;
    }

    public String getTransportadoraNome() {
        return transportadoraNome;
    }

    public void setTransportadoraNome(String transportadoraNome) {
        this.transportadoraNome = transportadoraNome;
    }
}
