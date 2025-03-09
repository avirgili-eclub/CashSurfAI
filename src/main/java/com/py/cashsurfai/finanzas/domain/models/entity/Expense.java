package com.py.cashsurfai.finanzas.domain.models.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String amount;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column
    private String invoiceNumber;

    @Column
    private String notes;

    @Column
    private String source;

    @Column
    private LocalDate createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_expense_group_id")
    private SharedExpenseGroup sharedExpenseGroup; // Relación opcional con grupo compartido

    public Expense(String monto, LocalDate localDate, String description, Category category, User user) {
        this.amount = monto;
        this.date = localDate;
        this.description = description;
        this.category = category;
        this.user = user;
    }
}