package com.py.lawbyteia.finanzas.domain.repository;

import com.py.lawbyteia.finanzas.domain.models.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserId(Long userId);
}