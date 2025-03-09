package com.py.cashsurfai.finanzas.domain.models.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "shared_expense_groups")
public class SharedExpenseGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // ej. "Viaje con Amigos", "Gastos con Usuario 2"

    @ManyToMany
    @JoinTable(
            name = "shared_expense_group_participants",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> participants; // Usuarios que comparten el grupo

    @OneToMany(mappedBy = "sharedExpenseGroup", cascade = CascadeType.ALL)
    private List<Expense> expenses; // Expensas asociadas al grupo

    @Column
    private LocalDate createdAt;

    // Constructores
    public SharedExpenseGroup() {}
    public SharedExpenseGroup(String name, List<User> participants) {
        this.name = name;
        this.participants = participants;
        this.createdAt = LocalDate.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<User> getParticipants() { return participants; }
    public void setParticipants(List<User> participants) { this.participants = participants; }
    public List<Expense> getExpenses() { return expenses; }
    public void setExpenses(List<Expense> expenses) { this.expenses = expenses; }
    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
}