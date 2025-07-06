package ru.iguana.integrationroles.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class RoleEntity implements Serializable {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

}
