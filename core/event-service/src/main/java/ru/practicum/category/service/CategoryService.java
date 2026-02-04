package ru.practicum.category.service;

import interaction.model.category.dto.CategoryDto;
import interaction.model.category.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    public CategoryDto add(NewCategoryDto newCategoryDto);

    public CategoryDto update(Integer categoryId, NewCategoryDto newCategoryDto);

    public void delete(Integer categoryId);

    List<CategoryDto> getCategories(Integer from, Integer size);

    CategoryDto getCategory(Integer catId);
}
