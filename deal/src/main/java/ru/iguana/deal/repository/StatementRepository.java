package ru.iguana.deal.repository;

import org.springframework.data.repository.CrudRepository;
import ru.iguana.deal.entity.Statement;

import java.util.UUID;

public interface StatementRepository extends CrudRepository<Statement, UUID> {
}
