package restaurant.site.services.application;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import restaurant.site.dto.CategoryDto;
import restaurant.site.entity.Category;
import restaurant.site.mapper.CategoryMapper;
import restaurant.site.exception.BusinessLogicException;
import restaurant.site.exception.DuplicateResourceException;
import restaurant.site.exception.NotFoundException;
import restaurant.site.repo.CategoryRepository;
import restaurant.site.repo.ItemRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    @CacheEvict(value = "category", allEntries = true)
    public CategoryDto.Response create(CategoryDto.Request request) {

        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("category.duplicate.name", request.getName());
        }
        Category category = categoryMapper.toEntity(request);

        category.setItems(new ArrayList<>()); // empty list, so the response shows [] and not null

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = "category", allEntries = true)
    public void delete(String name) {
        Category category = categoryRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new NotFoundException("category.not.found"));

        // one cheap query instead of loading every item of the category
        if (itemRepository.existsByCategoryId(category.getId())) {
            throw new BusinessLogicException("category.has.items");
        }

        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "category" , key = "'all'")
    public List<CategoryDto.Response> getAll(){
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse).collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    @Cacheable(value = "category" , key = "#name.toLowerCase()")
    public CategoryDto.Response getByName(String name){

        return categoryRepository.findByNameIgnoreCase(name)
                .map(categoryMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("category.not.found"));
    }


}
