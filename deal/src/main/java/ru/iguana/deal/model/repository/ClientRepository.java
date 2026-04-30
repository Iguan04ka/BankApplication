package ru.iguana.deal.model.repository;

import org.springframework.data.repository.CrudRepository;
import ru.iguana.deal.model.entity.Client;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends CrudRepository<Client, UUID> {
    Optional<Client> findByUserSub(String userSub);
}
