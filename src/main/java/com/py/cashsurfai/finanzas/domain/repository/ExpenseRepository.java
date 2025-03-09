package com.py.cashsurfai.finanzas.domain.repository;

import com.py.cashsurfai.finanzas.domain.models.entity.Expense;
import com.py.cashsurfai.finanzas.domain.models.entity.SharedExpenseGroup;
import com.py.cashsurfai.finanzas.domain.models.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserId(Long userId);
    List<Expense> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    List<Expense> findByUser(User user);
    List<Expense> findBySharedExpenseGroupIn(List<SharedExpenseGroup> groups);
}