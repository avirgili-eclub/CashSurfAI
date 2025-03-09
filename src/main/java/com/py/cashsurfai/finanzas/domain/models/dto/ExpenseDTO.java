package com.py.cashsurfai.finanzas.domain.models.dto;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@ToString
@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseDTO implements Serializable {
    private Long id;
    private String amount;
    private LocalDate date;
    private String description;
    private CategoryDTO category; // DTO anidado para Category
    private Long userId; // Solo el ID del usuario para simplicidad
    private String invoiceNumber;
    private String notes;
    private String source;
    private LocalDate createdAt;
    private Long sharedGroupId; // ID del grupo compartido (opcional)
}

