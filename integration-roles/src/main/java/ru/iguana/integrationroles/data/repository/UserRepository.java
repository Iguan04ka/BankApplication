package ru.iguana.integrationroles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.iguana.integrationroles.data.entity.UserEntity;

import java.util.List;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query("SELECT u.userKey.sub FROM UserEntity u " +
            "JOIN u.roles ur " +
            "JOIN ur.role r " +
            "WHERE r.name = :roleName")
    List<String> findUserLoginsByRoleName(@Param("roleName") String roleName);

}
