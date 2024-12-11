package ru.iguana.deal.repository;

import org.springframework.data.repository.CrudRepository;
import ru.iguana.deal.entity.Client;

import java.util.UUID;

public interface ClientRepository extends CrudRepository<Client, UUID> {
}
