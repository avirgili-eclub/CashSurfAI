package com.py.cashsurfai.finanzas.domain.repository;

import com.py.cashsurfai.finanzas.domain.models.entity.SharedExpenseGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SharedExpenseGroupRepository extends JpaRepository<SharedExpenseGroup, Long> {}