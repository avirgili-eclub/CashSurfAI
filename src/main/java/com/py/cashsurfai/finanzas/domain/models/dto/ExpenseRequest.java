package com.py.cashsurfai.finanzas.domain.models.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ExpenseRequest {
    private String amount;
    private LocalDate date;
    private String description;
    private String category;
    private Long userId;
    private Long sharedGroupId;
}
