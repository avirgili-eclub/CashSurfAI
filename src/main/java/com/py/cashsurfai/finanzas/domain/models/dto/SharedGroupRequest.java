package com.py.cashsurfai.finanzas.domain.models.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SharedGroupRequest {
    private String name;
    private List<Long> userIds;
}
