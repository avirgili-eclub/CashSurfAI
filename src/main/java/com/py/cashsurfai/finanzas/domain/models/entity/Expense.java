package com.py.cashsurfai.finanzas.domain.models.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String amount;
    private LocalDate date;
    private String description;
    private String category;
    private Long userId;

    // Constructores
    public Expense() {}

    public Expense(String amount, LocalDate date, String description, String category, Long userId) {
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.category = category;
        this.userId = userId;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}