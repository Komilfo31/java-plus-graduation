package ru.practicum.category.service;

import interaction.exceptions.exception.ConflictException;
import interaction.exceptions.exception.NotFoundException;
import interaction.exceptions.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import interaction.model.category.dto.CategoryDto;
import interaction.model.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.storage.CategoryRepository;
import ru.practicum.events.repository.EventRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Qualifier("CategoryServiceImpl")
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CategoryDto add(NewCategoryDto newCategoryDto) {

        if (newCategoryDto.getName() == null) {
            throw new ValidationException("Category name cannot be null");
        }
        // Проверка уникальности имени
        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new ConflictException("Category with name: " + newCategoryDto.getName() + " already exists.");
        }

        Category category = categoryMapper.toCategory(newCategoryDto);
        return categoryMapper.toCategoryDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryDto update(Integer categoryId, NewCategoryDto newCategoryDto) {
        Category oldCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id = " + categoryId + " не найдена.", log));

        if (!oldCategory.getName().equals(newCategoryDto.getName()) &&
                categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new ConflictException("Category with name: " + newCategoryDto.getName() + " already exists.");
        }

        oldCategory.setName(newCategoryDto.getName());
        return categoryMapper.toCategoryDto(categoryRepository.save(oldCategory));
    }

    @Override
    @Transactional
    public void delete(Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + categoryId + " was not found."));

        // Проверяем наличие связанных событий
        if (eventRepository.countByCategoryId(categoryId) > 0) {
            throw new ConflictException("Category is not empty");
        }

        categoryRepository.deleteById(categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories(Integer from, Integer size) {
        // Проверка параметров
        if (from < 0) {
            throw new ValidationException("Parameter 'from' must be >= 0");
        }
        if (size <= 0) {
            throw new ValidationException("Parameter 'size' must be > 0");
        }

        int page = (from / size);
        int pageFrom = from % size;

        Page<Category> categoryPage = categoryRepository.findAll(
                PageRequest.of(page, size, Sort.by("id").ascending())
        );

        List<Category> categories = categoryPage.getContent();

        if (pageFrom > 0 && !categories.isEmpty()) {
            if (pageFrom >= categories.size()) {
                categories = List.of();
            } else {
                categories = categories.subList(pageFrom, categories.size());
            }
        }

        return categories.stream()
                .map(categoryMapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategory(Integer catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found."));

        return categoryMapper.toCategoryDto(category);
    }
}
