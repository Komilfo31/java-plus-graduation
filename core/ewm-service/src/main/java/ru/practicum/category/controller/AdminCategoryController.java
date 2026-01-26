package ru.practicum.category.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.service.CategoryService;
import ru.practicum.validation.Marker;

import java.util.List;

@RestController
@RequestMapping(path = "/admin/categories")
@RequiredArgsConstructor
@Validated
public class AdminCategoryController {

    private final CategoryService categoryService;

    // POST /admin/categories
    @Validated(Marker.OnCreate.class)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody NewCategoryDto newCategoryDto) {
        return categoryService.add(newCategoryDto);
    }

    // PATCH /admin/categories/{catId}
    @Validated(Marker.OnUpdate.class)
    @PatchMapping("/{catId}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryDto updateCategory(@PathVariable @Positive Integer catId,
                                      @Valid @RequestBody NewCategoryDto updateCategoryDto) {
        return categoryService.update(catId, updateCategoryDto);
    }

    // DELETE /admin/categories/{catId}
    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable @Positive Integer catId) {
        categoryService.delete(catId);
    }

    // GET /categories
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CategoryDto> getCategories(
            @RequestParam(defaultValue = "0") @Min(0) Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {

        return categoryService.getCategories(from, size);
    }

    // GET /categories/{catId}
    @GetMapping("/{catId}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryDto getCategory(@PathVariable @Positive Integer catId) {
        return categoryService.getCategory(catId);
    }
}
