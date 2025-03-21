package ru.iguana.deal.model.repository;

import org.springframework.data.repository.CrudRepository;
import ru.iguana.deal.model.entity.Credit;

import java.util.UUID;

public interface CreditRepository extends CrudRepository<Credit, UUID> {
}
