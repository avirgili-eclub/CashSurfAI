package com.py.cashsurfai.finanzas.domain.repository;

import com.py.cashsurfai.finanzas.domain.models.entity.Category;
import com.py.cashsurfai.finanzas.domain.models.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    Optional<Category> findByNameAndUser(String name, User user);
}