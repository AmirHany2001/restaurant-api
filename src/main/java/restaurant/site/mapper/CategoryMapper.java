package restaurant.site.mapper;


import org.mapstruct.Mapper;
import restaurant.site.dto.CategoryDto;
import restaurant.site.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryDto.Response toResponse (Category category);

    Category toEntity(CategoryDto.Request request);

}
