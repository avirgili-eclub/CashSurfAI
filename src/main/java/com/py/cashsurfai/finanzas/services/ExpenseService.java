package com.py.cashsurfai.finanzas.services;

import com.py.cashsurfai.finanzas.domain.models.dto.ExpenseDTO;
import com.py.cashsurfai.finanzas.domain.models.entity.Expense;
import com.py.cashsurfai.finanzas.domain.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    public ExpenseDTO saveExpense(Expense expense) {
        Expense savedExpense = expenseRepository.save(expense);
        return mapToDTO(savedExpense);
    }

    public List<ExpenseDTO> getExpensesByUser(Long userId) {
        List<Expense> expenses = expenseRepository.findByUserId(userId);
        return expenses.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private ExpenseDTO mapToDTO(Expense expense) {
        return new ExpenseDTO(
                expense.getId(),
                expense.getAmount(),
                expense.getDate(),
                expense.getDescription(),
                expense.getCategory(),
                expense.getUserId()
        );
    }
}