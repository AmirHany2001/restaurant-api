package restaurant.site.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import restaurant.site.dto.CategoryDto;
import restaurant.site.services.application.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@NullMarked
public class CategoryController {
    private final CategoryService categoryService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/createCategory")
    public ResponseEntity<CategoryDto.Response> create(@Valid @RequestBody CategoryDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @GetMapping("/getCategories")
    public ResponseEntity<List<CategoryDto.Response>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @GetMapping("/getCategoryByName/{name}")
    public ResponseEntity<CategoryDto.Response> getByName(@PathVariable @NotBlank String name) {
        return ResponseEntity.ok(categoryService.getByName(name));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/deleteCategory/{name}")
    public ResponseEntity<Void> delete(@PathVariable @NotBlank String name) {
        categoryService.delete(name);
        return ResponseEntity.accepted().eTag("category.deleted.successfully").build();
    }




}
