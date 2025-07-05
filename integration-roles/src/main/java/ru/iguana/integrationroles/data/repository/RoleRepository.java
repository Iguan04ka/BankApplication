package ru.iguana.integrationroles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.iguana.integrationroles.data.entity.RoleEntity;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByName(String name);
}
