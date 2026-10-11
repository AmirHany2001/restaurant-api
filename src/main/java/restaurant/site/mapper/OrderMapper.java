package restaurant.site.mapper;


import org.mapstruct.Mapper;
import restaurant.site.dto.OrderDto;
import restaurant.site.entity.Order;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderDto.Response toResponse (Order order);

    Order toEntity(OrderDto.Request request);

}
