package ru.iguana.integrationroles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.iguana.integrationroles.data.entity.TwoFactorChallengeEntity;
import ru.iguana.integrationroles.data.entity.TwoFactorPurpose;

import java.util.Optional;

public interface TwoFactorChallengeRepository extends JpaRepository<TwoFactorChallengeEntity, Long> {

    Optional<TwoFactorChallengeEntity>
            findFirstByUserSubAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                    String userSub, TwoFactorPurpose purpose);

    @Modifying
    @Query("UPDATE TwoFactorChallengeEntity c SET c.used = true " +
           "WHERE c.userSub = :sub AND c.purpose = :purpose AND c.used = false")
    void invalidateAllByUserSubAndPurpose(@Param("sub") String sub,
                                          @Param("purpose") TwoFactorPurpose purpose);
}
