package restaurant.site.services.application;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import restaurant.site.dto.OrderDto;
import restaurant.site.dto.OrderItemDto;
import restaurant.site.entity.Item;
import restaurant.site.entity.Order;
import restaurant.site.entity.OrderItem;
import restaurant.site.entity.User;
import restaurant.site.mapper.OrderMapper;
import restaurant.site.exception.BusinessLogicException;
import restaurant.site.exception.NotFoundException;
import restaurant.site.repo.ItemRepository;
import restaurant.site.repo.OrderRepository;
import restaurant.site.repo.UserRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService;

    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public OrderDto.Response createOrder(OrderDto.Request request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(()->new NotFoundException("user.not.found"));

        if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
            throw new BusinessLogicException("order.size");
        }


        Order order = new Order();
        order.setUser(user);
        order.setStatus(Order.Status.pending);

        List<OrderItem> lines = new ArrayList<>();
        double total = 0.0;

        for(OrderItemDto items : request.getOrderItems()) {

            Item item = itemRepository.findByNameIgnoreCase(items.getItemName())
                    .orElseThrow(()->new NotFoundException("item.not.found"));


            if(item.getStatus() != Item.Status.available){
                throw new BusinessLogicException("order.item.unavailable");
            }

            if(items.getQuantity() == null ||items.getQuantity() <= 0  ) {
                throw new BusinessLogicException("item.quantity.min");
            }

            Double itemPrice = itemService.getItemPriceBySize(item.getId(),items.getSize());

            if( itemPrice == null){
                throw new BusinessLogicException("order.size.available");
            }


            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(order);
            orderItem.setItem(item);
            orderItem.setSize(items.getSize().name());
            orderItem.setQuantity(items.getQuantity());
            orderItem.setUnitPriceAtOrder(itemPrice);
            orderItem.setNotes(items.getNotes());

            lines.add(orderItem);
            total += itemPrice * items.getQuantity() ;
        }

        order.setOrderItems(lines);
        order.setTotalPrice(total);

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Cacheable(value = "orders", key = "#id")
    public OrderDto.Response getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("order.not.found"));
        return orderMapper.toResponse(order);
    }

    @Cacheable(value = "orders", key = "'user: ' + #userId")
    public List<OrderDto.Response> getOrdersByUser(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("{user.not.found}");
        }
        return orderRepository.findByUserId(userId)
                .stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "orders", key = "#status")
    public List<OrderDto.Response> getOrdersByStatus(Order.Status status) {
        return orderRepository.findByStatus(status)
                .stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "orders", key = "'all'")
    public List<OrderDto.Response> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public OrderDto.Response updateOrderStatus(long id, Order.Status status) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("order.not.found"));


        if (status.ordinal() <= order.getStatus().ordinal()) {
            throw new BusinessLogicException("order.status.change.invalid");
        }

        order.setStatus(status);
        return orderMapper.toResponse(order);
    }


    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public void cancelOrder(long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("order.not.found"));


        if (order.getStatus() != Order.Status.pending) {
            throw new BusinessLogicException("order.cancellation.failed");
        }

        orderRepository.delete(order);
    }

}
