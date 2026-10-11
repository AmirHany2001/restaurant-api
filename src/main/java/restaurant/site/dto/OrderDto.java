package restaurant.site.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import restaurant.site.entity.Order;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request{
        @NotNull(message = "user.not.found")
        private Long userId;

        @NotEmpty(message = "order.size")
        @Valid
        List<OrderItemDto>  orderItems;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response{
        private Long orderId;
        private LocalDateTime orderDate;
        private List<OrderItemDto> orderItems;
        private Double totalPrice ;
        private Order.Status status ;
    }

}
