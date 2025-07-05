package ru.iguana.integrationroles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.iguana.integrationroles.data.entity.UserRole;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUserId(Long userId);
}