package ru.iguana.deal.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.iguana.deal.model.entity.UserDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDocumentRepository extends JpaRepository<UserDocument, UUID> {

    List<UserDocument> findAllByClientId(UUID clientId);

    Optional<UserDocument> findByClientIdAndDocumentType(UUID clientId, String documentType);
}
