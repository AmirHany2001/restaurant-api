package restaurant.site.services.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import restaurant.site.dto.OrderDto;
import restaurant.site.dto.OrderItemDto;
import restaurant.site.entity.Item;
import restaurant.site.entity.Order;
import restaurant.site.entity.OrderItem;
import restaurant.site.entity.User;
import restaurant.site.exception.BusinessLogicException;
import restaurant.site.exception.NotFoundException;
import restaurant.site.mapper.OrderMapper;
import restaurant.site.repo.ItemRepository;
import restaurant.site.repo.OrderRepository;
import restaurant.site.repo.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Plain unit test: no Spring, no database. The repositories are mocks; everything else is real.
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderMapper orderMapper;
    @Mock UserRepository userRepository;
    @Mock ItemRepository itemRepository;
    @InjectMocks OrderService orderService;

    // ---------- helpers ----------

    private static User user(String email) {
        User user = new User();
        user.setId(7L);
        user.setEmail(email);
        user.setRole(User.Role.USER);
        return user;
    }

    private static Item item(String name, Item.Status status, Map<Item.ItemSize, BigDecimal> price) {
        Item item = new Item();
        item.setName(name);
        item.setStatus(status);
        item.setPrice(price);
        return item;
    }

    private static OrderItemDto line(String itemName, Item.ItemSize size, Integer quantity) {
        OrderItemDto line = new OrderItemDto();
        line.setItemName(itemName);
        line.setSize(size);
        line.setQuantity(quantity);
        return line;
    }

    private static OrderDto.Request request(OrderItemDto... lines) {
        return new OrderDto.Request(List.of(lines));
    }

    private static Order orderOf(User owner, Order.Status status) {
        Order order = new Order();
        order.setUser(owner);
        order.setStatus(status);
        return order;
    }

    private static void assertCode(Throwable thrown, Class<? extends RuntimeException> type, String code) {
        assertThat(thrown).isInstanceOf(type).hasFieldOrPropertyWithValue("code", code);
    }

    // ---------- createOrder ----------

    @Test
    void createOrder_addsMoneyExactly_usingThePricesFromTheItemRows() {
        // 0.10 * 3 is 0.30000000000000004 as a double, so this test fails if money ever goes back to double
        Item cola = item("Cola", Item.Status.available, Map.of(Item.ItemSize.small, new BigDecimal("0.10")));
        Item juice = item("Juice", Item.Status.available, Map.of(Item.ItemSize.large, new BigDecimal("0.20")));
        User sara = user("sara@example.com");
        when(userRepository.findByEmailIgnoreCase("sara@example.com")).thenReturn(Optional.of(sara));
        when(itemRepository.findByNameIgnoreCase("Cola")).thenReturn(Optional.of(cola));
        when(itemRepository.findByNameIgnoreCase("Juice")).thenReturn(Optional.of(juice));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrder(request(
                line("Cola", Item.ItemSize.small, 3),
                line("Juice", Item.ItemSize.large, 1)), "sara@example.com");

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        Order order = saved.getValue();
        assertThat(order.getTotalPrice()).isEqualTo(new BigDecimal("0.50"));
        assertThat(order.getOrderItems()).extracting(OrderItem::getUnitPriceAtOrder)
                .containsExactly(new BigDecimal("0.10"), new BigDecimal("0.20"));
        assertThat(order.getStatus()).isEqualTo(Order.Status.pending);
        assertThat(order.getUser()).isSameAs(sara); // the customer is the token's user, nobody else
    }

    @Test
    void createOrder_refusesAnItemThatIsNotAvailable() {
        Item pizza = item("Pizza", Item.Status.not_available, Map.of(Item.ItemSize.small, BigDecimal.TEN));
        when(userRepository.findByEmailIgnoreCase("sara@example.com")).thenReturn(Optional.of(user("sara@example.com")));
        when(itemRepository.findByNameIgnoreCase("Pizza")).thenReturn(Optional.of(pizza));

        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() ->
                orderService.createOrder(request(line("Pizza", Item.ItemSize.small, 1)), "sara@example.com"));

        assertCode(thrown, BusinessLogicException.class, "order.item.unavailable");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_refusesASizeThatIsNotSold() {
        Item pizza = item("Pizza", Item.Status.available, Map.of(Item.ItemSize.small, BigDecimal.TEN));
        when(userRepository.findByEmailIgnoreCase("sara@example.com")).thenReturn(Optional.of(user("sara@example.com")));
        when(itemRepository.findByNameIgnoreCase("Pizza")).thenReturn(Optional.of(pizza));

        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() ->
                orderService.createOrder(request(line("Pizza", Item.ItemSize.large, 1)), "sara@example.com"));

        assertCode(thrown, BusinessLogicException.class, "order.size.available");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_refusesAnUnknownItem() {
        when(userRepository.findByEmailIgnoreCase("sara@example.com")).thenReturn(Optional.of(user("sara@example.com")));
        when(itemRepository.findByNameIgnoreCase("Ghost")).thenReturn(Optional.empty());

        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() ->
                orderService.createOrder(request(line("Ghost", Item.ItemSize.small, 1)), "sara@example.com"));

        assertCode(thrown, NotFoundException.class, "item.not.found.name");
    }

    @Test
    void createOrder_refusesAZeroQuantity() {
        Item pizza = item("Pizza", Item.Status.available, Map.of(Item.ItemSize.small, BigDecimal.TEN));
        when(userRepository.findByEmailIgnoreCase("sara@example.com")).thenReturn(Optional.of(user("sara@example.com")));
        when(itemRepository.findByNameIgnoreCase("Pizza")).thenReturn(Optional.of(pizza));

        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() ->
                orderService.createOrder(request(line("Pizza", Item.ItemSize.small, 0)), "sara@example.com"));

        assertCode(thrown, BusinessLogicException.class, "item.quantity.min");
    }

    @Test
    void createOrder_refusesAnEmptyOrder() {
        when(userRepository.findByEmailIgnoreCase("sara@example.com")).thenReturn(Optional.of(user("sara@example.com")));

        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() ->
                orderService.createOrder(request(), "sara@example.com"));

        assertCode(thrown, BusinessLogicException.class, "order.size");
        verify(orderRepository, never()).save(any());
    }

    // ---------- cancelOrder ----------

    @Test
    void cancelOrder_theOwnerCancelsAPendingOrder_andTheOrderIsKept() {
        Order order = orderOf(user("sara@example.com"), Order.Status.pending);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.cancelOrder(1L, "SARA@example.com", false); // e-mail case does not matter

        assertThat(order.getStatus()).isEqualTo(Order.Status.cancelled);
        verify(orderRepository, never()).delete(any()); // history is kept, nothing is deleted
    }

    @Test
    void cancelOrder_anotherCustomerGetsNotFound_notForbidden() {
        // "not found" (not 403) so a stranger cannot even tell that the order exists
        Order order = orderOf(user("sara@example.com"), Order.Status.pending);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() ->
                orderService.cancelOrder(1L, "omar@example.com", false));

        assertCode(thrown, NotFoundException.class, "order.not.found");
        assertThat(order.getStatus()).isEqualTo(Order.Status.pending);
    }

    @Test
    void cancelOrder_anAdminCanCancelSomeoneElsesOrder() {
        Order order = orderOf(user("sara@example.com"), Order.Status.pending);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.cancelOrder(1L, "admin@example.com", true);

        assertThat(order.getStatus()).isEqualTo(Order.Status.cancelled);
    }

    @Test
    void cancelOrder_onlyWhileItIsPending() {
        Order order = orderOf(user("sara@example.com"), Order.Status.approved);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() ->
                orderService.cancelOrder(1L, "sara@example.com", false));

        assertCode(thrown, BusinessLogicException.class, "order.cancellation.failed");
        assertThat(order.getStatus()).isEqualTo(Order.Status.approved);
    }

    // ---------- updateOrderStatus ----------

    @Test
    void updateOrderStatus_followsTheAllowedMoves() {
        Order order = orderOf(user("sara@example.com"), Order.Status.pending);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.updateOrderStatus(1L, Order.Status.approved);

        assertThat(order.getStatus()).isEqualTo(Order.Status.approved);
    }

    @Test
    void updateOrderStatus_refusesToReopenAFinishedOrder() {
        Order order = orderOf(user("sara@example.com"), Order.Status.completed);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() ->
                orderService.updateOrderStatus(1L, Order.Status.pending));

        assertCode(thrown, BusinessLogicException.class, "order.status.change.invalid");
        assertThat(order.getStatus()).isEqualTo(Order.Status.completed);
    }

    @Test
    void getOrderById_unknownIdIsNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L)).isInstanceOf(NotFoundException.class);
    }
}
