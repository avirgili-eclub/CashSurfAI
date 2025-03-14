package com.py.cashsurfai.finanzas.services;

import com.py.cashsurfai.finanzas.domain.models.entity.Category;
import com.py.cashsurfai.finanzas.domain.models.entity.User;
import com.py.cashsurfai.finanzas.domain.repository.CategoryRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // Crear o encontrar categoría con emoji dinámico
    public Category findOrCreateCategory(String name, String description, String emoji, User user) {
        return categoryRepository.findByNameAndUser(name, user)
                .orElseGet(() -> {
                    Category newCategory = new Category(name, description, emoji, user);
                    return categoryRepository.save(newCategory);
                });
    }

    public Category findOrCreateCategory(String name, String emoji, User user) {
        return categoryRepository.findByNameAndUser(name, user)
                .orElseGet(() -> {
                    Category newCategory = new Category(name, emoji, user);
                    return categoryRepository.save(newCategory);
                });
    }


    public Category findOrCreateCategory(String name, String emoji) {
        return categoryRepository.findByName(name)
                .orElseGet(() -> categoryRepository.save(new Category(name, emoji)));
    }

}
