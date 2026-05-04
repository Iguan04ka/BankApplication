package ru.iguana.integrationroles.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserEntity implements Serializable {

    @Id
    @GeneratedValue()
    private Long id;

    @Embedded
    private UserKey userKey;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email")
    private String email;

    @Column(name = "is_blocked", nullable = false)
    private boolean blocked;

    @Column(name = "two_factor_enabled", nullable = false,
            columnDefinition = "boolean NOT NULL DEFAULT false")
    private boolean twoFactorEnabled;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> roles = new HashSet<>();
}
