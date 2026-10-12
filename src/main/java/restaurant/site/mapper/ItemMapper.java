package restaurant.site.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import restaurant.site.dto.ItemDto;
import restaurant.site.entity.Item;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    // the category name lives one level down: item.category.name
    @Mapping(source = "category.name", target = "categoryName")
    ItemDto.Response toResponse (Item item);

    // id comes from the database and the category is loaded and set by ItemService
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    Item toEntity(ItemDto.adminRequest request);

}
