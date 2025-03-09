package com.py.cashsurfai.finanzas.domain.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column
    private String email;

    // Relación con expensas individuales
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Expense> expenses;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Category> categories;

    // Relación con gastos compartidos
    @ManyToMany(mappedBy = "participants")
    private List<SharedExpenseGroup> sharedExpenseGroups;

    // Constructores adicionales
    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

}