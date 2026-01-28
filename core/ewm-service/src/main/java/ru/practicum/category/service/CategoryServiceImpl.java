package ru.practicum.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.storage.CategoryRepository;
import ru.practicum.handling.exception.ConflictException;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.handling.exception.NotFoundException;

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

        // Удаляем категорию
        categoryRepository.deleteById(categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories(Integer from, Integer size) {
        // Логика пагинации: from/size преобразуется в PageRequest
        int page = from / size;

        List<Category> categories = categoryRepository.findAll(PageRequest.of(page, size))
                .getContent();

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
