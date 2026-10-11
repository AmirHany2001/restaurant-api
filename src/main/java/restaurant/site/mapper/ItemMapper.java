package restaurant.site.mapper;


import org.mapstruct.Mapper;
import restaurant.site.dto.ItemDto;
import restaurant.site.entity.Item;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    ItemDto.Response toResponse (Item item);


    Item toEntity(ItemDto.adminRequest request);

}
