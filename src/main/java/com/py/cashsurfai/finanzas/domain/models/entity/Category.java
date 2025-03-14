package com.py.cashsurfai.finanzas.domain.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String description;

    @Column(nullable = false)
    private String emoji;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Constructor específico para name y emoji
    public Category(String name, String emoji) {
        this.name = name;
        this.emoji = emoji;
    }

    public Category(String name, String emoji, User user) {
        this.name = name;

        this.emoji = emoji;
        this.user = user;
    }

    public Category(String name, String description, String emoji, User user) {
        this.name = name;
        this.description = description;
        this.emoji = emoji;
        this.user = user;
    }
}