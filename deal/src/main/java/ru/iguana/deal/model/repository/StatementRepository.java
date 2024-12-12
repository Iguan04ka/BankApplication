package ru.iguana.deal.model.repository;

import org.springframework.data.repository.CrudRepository;
import ru.iguana.deal.model.entity.Statement;

import java.util.UUID;

public interface StatementRepository extends CrudRepository<Statement, UUID> {
}
