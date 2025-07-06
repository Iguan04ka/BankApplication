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
    private Long id;

    @Embedded
    private UserKey userKey;

    @Column(name = "is_blocked", nullable = false)
    private boolean blocked;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> roles = new HashSet<>();
}
