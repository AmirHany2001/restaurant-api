package restaurant.site.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import restaurant.site.dto.MessageResponse;
import restaurant.site.dto.OrderDto;
import restaurant.site.entity.Order;
import restaurant.site.services.application.OrderService;
import restaurant.site.services.language.ChooseLanguageService;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@NullMarked
public class OrderController {
    private final OrderService orderService;
    private final ChooseLanguageService chooseLanguageService;

    @PostMapping
    public ResponseEntity<OrderDto.Response> createOrder(@Valid @RequestBody OrderDto.Request request,
                                                         Authentication authentication) {
        // the customer is whoever owns the token, never a userId sent by the client
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request, authentication.getName()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto.Response> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // GET /orders                  -> all orders
    // GET /orders?status=pending   -> only the orders with that status
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<OrderDto.Response>> getOrders(@RequestParam(required = false) Order.@Nullable Status status) {
        return ResponseEntity.ok(status == null ? orderService.getAllOrders() : orderService.getOrdersByStatus(status));
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(#userId)")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDto.Response>> getOrdersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status/{status}")
    public ResponseEntity<OrderDto.Response> updateOrderStatus(@PathVariable long id, @PathVariable Order.Status status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    // an action, not a plain update: only the owner or an admin, and only while the order is pending
    @PostMapping("/{id}/cancel")
    public ResponseEntity<MessageResponse> cancelOrder(@PathVariable long id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));
        orderService.cancelOrder(id, authentication.getName(), isAdmin);
        return ResponseEntity.ok(new MessageResponse(
                chooseLanguageService.getAcceptedLanguageMessage("order.status.cancelled")));
    }

}
