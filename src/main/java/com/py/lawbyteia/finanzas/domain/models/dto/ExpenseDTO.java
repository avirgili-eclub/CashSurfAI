package com.py.lawbyteia.finanzas.domain.models.dto;

import java.time.LocalDate;

public class ExpenseDTO {
    private Long id;
    private double amount;
    private LocalDate date;
    private String description;
    private String category;
    private Long userId;

    // Constructores
    public ExpenseDTO() {}

    public ExpenseDTO(Long id, double amount, LocalDate date, String description, String category, Long userId) {
        this.id = id;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.category = category;
        this.userId = userId;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}