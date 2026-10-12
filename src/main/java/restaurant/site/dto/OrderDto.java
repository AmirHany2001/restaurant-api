package restaurant.site.dto;

import java.math.BigDecimal;
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
        @NotEmpty(message = "order.size")
        @Valid
        private List<OrderItemDto> orderItems;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response{
        private Long orderId;
        private LocalDateTime orderDate;
        private List<OrderItemDto> orderItems;
        private BigDecimal totalPrice;
        private Order.Status status ;
    }

}
