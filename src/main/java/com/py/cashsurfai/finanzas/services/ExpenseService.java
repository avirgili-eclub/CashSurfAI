package com.py.cashsurfai.finanzas.services;
import com.py.cashsurfai.finanzas.domain.models.dto.CategoryDTO;
import com.py.cashsurfai.finanzas.domain.models.dto.ExpenseDTO;
import com.py.cashsurfai.finanzas.domain.models.entity.Expense;
import com.py.cashsurfai.finanzas.domain.models.entity.SharedExpenseGroup;
import com.py.cashsurfai.finanzas.domain.models.entity.User;
import com.py.cashsurfai.finanzas.domain.repository.ExpenseRepository;
import com.py.cashsurfai.finanzas.domain.repository.SharedExpenseGroupRepository;
import com.py.cashsurfai.finanzas.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final SharedExpenseGroupRepository sharedExpenseGroupRepository;

    @Autowired
    public ExpenseService(ExpenseRepository expenseRepository,
                          UserRepository userRepository, SharedExpenseGroupRepository sharedExpenseGroupRepository) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.sharedExpenseGroupRepository = sharedExpenseGroupRepository;
    }

    // Guardar una expensa y devolver DTO
    public ExpenseDTO saveExpense(Expense expense) {
        Expense savedExpense = expenseRepository.save(expense);
        return mapToDTO(savedExpense);
    }

    public List<ExpenseDTO> saveAllExpenses(List<Expense> expenses) {
        List<Expense> savedExpenses = expenseRepository.saveAll(expenses);
        return savedExpenses.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ExpenseDTO convertToDTO(Expense expense) {
        CategoryDTO categoryDTO = new CategoryDTO(
                expense.getCategory().getId(),
                expense.getCategory().getName(),
                expense.getCategory().getEmoji()
        );

        ExpenseDTO dto = new ExpenseDTO();
        dto.setId(expense.getId());
        dto.setAmount(expense.getAmount());
        dto.setDate(expense.getDate());
        dto.setDescription(expense.getDescription());
        dto.setCategory(categoryDTO);
        dto.setUserId(expense.getUser().getId());
        dto.setSource(expense.getSource());
        dto.setSharedGroupId(expense.getSharedExpenseGroup() != null ? expense.getSharedExpenseGroup().getId() : null);
        // invoiceNumber, notes y createdAt quedan null por defecto si no los seteas
        return dto;
    }

    // Obtener expensas por usuario (individuales y compartidas)
    public List<ExpenseDTO> getExpensesByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Expensas individuales
        List<Expense> individualExpenses = expenseRepository.findByUserId(userId);

        // Expensas compartidas
        List<Expense> sharedExpenses = expenseRepository.findBySharedExpenseGroupIn(user.getSharedExpenseGroups());

        // Combinar y mapear a DTO
        individualExpenses.addAll(sharedExpenses);
        return individualExpenses.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Crear un grupo de gastos compartidos
    public SharedExpenseGroup createSharedExpenseGroup(String name, List<Long> userIds) {
        List<User> participants = userRepository.findAllById(userIds);
        if (participants.size() != userIds.size()) {
            throw new IllegalArgumentException("Some users not found");
        }
        SharedExpenseGroup group = new SharedExpenseGroup(name, participants);
        return sharedExpenseGroupRepository.save(group);
    }

    // Mapear Expense a ExpenseDTO
    private ExpenseDTO mapToDTO(Expense expense) {
        CategoryDTO categoryDTO = new CategoryDTO(
                expense.getCategory().getId(),
                expense.getCategory().getName(),
                expense.getCategory().getEmoji()
        );
        Long sharedGroupId = expense.getSharedExpenseGroup() != null ? expense.getSharedExpenseGroup().getId() : null;

        return new ExpenseDTO(
                expense.getId(),
                expense.getAmount(),
                expense.getDate(),
                expense.getDescription(),
                categoryDTO,
                expense.getUser().getId(),
                expense.getInvoiceNumber(),
                expense.getNotes(),
                expense.getSource(),
                expense.getCreatedAt(),
                sharedGroupId
        );
    }
}