package restaurant.site.services.application;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import restaurant.site.dto.CategoryDto;
import restaurant.site.entity.Category;
import restaurant.site.mapper.CategoryMapper;
import restaurant.site.exception.BusinessLogicException;
import restaurant.site.exception.DuplicateResourceException;
import restaurant.site.exception.NotFoundException;
import restaurant.site.repo.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @CacheEvict(value = "category", key = "'all'")
    public CategoryDto.Response create(CategoryDto.Request request) {

        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("category.duplicate.name");
        }
        Category category = categoryMapper.toEntity(request);

        category.setItems(null);

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @CacheEvict(value = "category", allEntries = true)
    public void delete(String name) {
        Category category = categoryRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new NotFoundException("category.not.found"));

        if(!category.getItems().isEmpty()){
            throw new BusinessLogicException("category.has.items");
        }

        categoryRepository.delete(category);
    }

    @Cacheable(value = "category" , key = "'all'")
    public List<CategoryDto.Response> getAll(){
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse).collect(Collectors.toList());
    }


    @Cacheable(value = "category" , key = "#name")
    public CategoryDto.Response getByName(String name){

        return categoryRepository.findByNameIgnoreCase(name)
                .map(categoryMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("category.not.found"));
    }


}
