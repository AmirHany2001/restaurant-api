package restaurant.site.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import restaurant.site.dto.OrderDto;
import restaurant.site.dto.OrderItemDto;
import restaurant.site.entity.Order;
import restaurant.site.entity.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    // names differ between the entity and the DTO, so MapStruct needs to be told
    @Mapping(source = "id", target = "orderId")
    @Mapping(source = "createdAt", target = "orderDate")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.fullName", target = "customerName")
    @Mapping(source = "user.phoneNumber", target = "customerPhone")
    @Mapping(source = "user.address", target = "deliveryAddress")
    OrderDto.Response toResponse (Order order);

    // MapStruct uses this method automatically for every element of orderItems
    @Mapping(source = "item.name", target = "itemName")
    OrderItemDto toItemDto(OrderItem orderItem);

}
