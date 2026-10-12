package restaurant.site.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import restaurant.site.dto.OrderDto;
import restaurant.site.entity.Order;
import restaurant.site.services.application.OrderService;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@NullMarked
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/createOrder")
    public ResponseEntity<OrderDto.Response> createOrder(@Valid @RequestBody OrderDto.Request request,
                                                         Authentication authentication) {
        // the customer is whoever owns the token, never a userId sent by the client
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request, authentication.getName()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getOrderById/{id}")
    public ResponseEntity<OrderDto.Response> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }


    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(#userId)")
    @GetMapping("/getOrdersByUser/{userId}")
    public ResponseEntity<List<OrderDto.Response>> getOrdersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getOrdersByStatus/{status}")
    public ResponseEntity<List<OrderDto.Response>> getOrdersByStatus(@PathVariable Order.Status status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getAllOrders")
    public ResponseEntity<List<OrderDto.Response>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/updateOrderState/{id}/{status}")
    public ResponseEntity<OrderDto.Response> updateOrderStatus(@PathVariable long id, @PathVariable Order.Status status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    @DeleteMapping("/cancelOrder/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable long id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));
        orderService.cancelOrder(id, authentication.getName(), isAdmin);
        return ResponseEntity.accepted().eTag("order.status.cancelled").build();
    }

}
