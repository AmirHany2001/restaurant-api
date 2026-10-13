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
import restaurant.site.dto.MessageResponse;
import restaurant.site.services.application.CategoryService;
import restaurant.site.services.language.ChooseLanguageService;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@NullMarked
public class CategoryController {
    private final CategoryService categoryService;
    private final ChooseLanguageService chooseLanguageService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CategoryDto.Response> create(@Valid @RequestBody CategoryDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<CategoryDto.Response>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @GetMapping("/{name}")
    public ResponseEntity<CategoryDto.Response> getByName(@PathVariable @NotBlank String name) {
        return ResponseEntity.ok(categoryService.getByName(name));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{name}")
    public ResponseEntity<MessageResponse> delete(@PathVariable @NotBlank String name) {
        categoryService.delete(name);
        return ResponseEntity.ok(new MessageResponse(
                chooseLanguageService.getAcceptedLanguageMessage("category.deleted.successfully")));
    }

}
