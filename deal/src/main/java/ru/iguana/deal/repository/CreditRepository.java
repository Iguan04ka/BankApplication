package ru.iguana.deal.repository;

import org.springframework.data.repository.CrudRepository;
import ru.iguana.deal.entity.Credit;

import java.util.UUID;

public interface CreditRepository extends CrudRepository<Credit, UUID> {
}
