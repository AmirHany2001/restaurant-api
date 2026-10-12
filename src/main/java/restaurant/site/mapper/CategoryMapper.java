package restaurant.site.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import restaurant.site.dto.CategoryDto;
import restaurant.site.entity.Category;

// uses = ItemMapper: the items inside a category are converted with ItemMapper.toResponse,
// so they get the same mapping rules (including categoryName) as everywhere else
@Mapper(componentModel = "spring", uses = ItemMapper.class)
public interface CategoryMapper {

    CategoryDto.Response toResponse (Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "items", ignore = true)
    Category toEntity(CategoryDto.Request request);

}
