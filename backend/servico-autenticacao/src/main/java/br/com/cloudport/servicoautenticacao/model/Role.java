package br.com.cloudport.servicoautenticacao.model;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.Set;
import java.util.HashSet; // Make sure to import this


@Entity
@Getter
@Setter
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @ManyToMany(mappedBy = "roles")
    private Set<UserRole> userRoles = new HashSet<>();

    // Default constructor
    public Role() {
    }

    public Role(String name) {
        this.name = name;
    }
}
