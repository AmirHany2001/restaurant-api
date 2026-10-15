package restaurant.site.services.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import restaurant.site.entity.Item;
import restaurant.site.exception.BusinessLogicException;
import restaurant.site.exception.NotFoundException;
import restaurant.site.mapper.ItemMapper;
import restaurant.site.repo.CategoryRepository;
import restaurant.site.repo.ItemRepository;
import restaurant.site.repo.OrderItemRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock ItemRepository itemRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock ItemMapper itemMapper;
    @InjectMocks ItemService itemService;

    private static Item itemWithSmallPrice(String price) {
        Item item = new Item();
        item.setName("Pizza");
        item.setPrice(new HashMap<>(Map.of(Item.ItemSize.small, new BigDecimal(price))));
        return item;
    }

    private void assertCode(Throwable thrown, Class<? extends RuntimeException> type, String code) {
        assertThat(thrown).isInstanceOf(type).hasFieldOrPropertyWithValue("code", code);
    }

    // ---------- changePrice ----------

    @Test
    void changePrice_tenAndTenPointZeroZeroAreTheSamePrice() {
        // BigDecimal.equals says 10 != 10.00; the service rounds first so "no change" is detected correctly
        Item item = itemWithSmallPrice("10.00");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Throwable thrown = catchThrowable(() ->
                itemService.changePrice(1L, Map.of(Item.ItemSize.small, new BigDecimal("10"))));

        assertCode(thrown, BusinessLogicException.class, "item.price.duplicate");
    }

    @Test
    void changePrice_roundsToTwoDecimals() {
        Item item = itemWithSmallPrice("10.00");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        itemService.changePrice(1L, Map.of(Item.ItemSize.small, new BigDecimal("10.555")));

        assertThat(item.getPrice()).containsOnly(Map.entry(Item.ItemSize.small, new BigDecimal("10.56")));
    }

    @Test
    void changePrice_aSizeThatIsLeftOutIsNoLongerSold() {
        Item item = itemWithSmallPrice("10.00");
        item.getPrice().put(Item.ItemSize.large, new BigDecimal("20.00"));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        itemService.changePrice(1L, Map.of(Item.ItemSize.small, new BigDecimal("12")));

        assertThat(item.getPrice()).containsOnlyKeys(Item.ItemSize.small);
    }

    @Test
    void changePrice_refusesZeroNegativeAndMissingPrices() {
        Item item = itemWithSmallPrice("10.00");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        Map<Item.ItemSize, BigDecimal> withNull = new HashMap<>();
        withNull.put(Item.ItemSize.small, null);

        for (Map<Item.ItemSize, BigDecimal> bad : java.util.List.of(
                Map.of(Item.ItemSize.small, BigDecimal.ZERO),
                Map.of(Item.ItemSize.small, new BigDecimal("-5")),
                withNull)) {
            assertCode(catchThrowable(() -> itemService.changePrice(1L, bad)), BusinessLogicException.class, "item.price.min");
        }
        assertThat(item.getPrice().get(Item.ItemSize.small)).isEqualTo(new BigDecimal("10.00")); // untouched
    }

    @Test
    void changePrice_refusesAnEmptyPriceList() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(itemWithSmallPrice("10.00")));

        Throwable thrown = catchThrowable(() -> itemService.changePrice(1L, Map.of()));

        assertCode(thrown, BusinessLogicException.class, "item.price.required");
    }

    // ---------- deleteItem ----------

    @Test
    void deleteItem_refusesAnItemThatIsInPastOrders() {
        when(itemRepository.existsById(5L)).thenReturn(true);
        when(orderItemRepository.existsByItemId(5L)).thenReturn(true);

        Throwable thrown = catchThrowable(() -> itemService.deleteItem(5L));

        assertCode(thrown, BusinessLogicException.class, "item.delete.has.orders");
        verify(itemRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteItem_removesAnItemNobodyOrdered() {
        when(itemRepository.existsById(5L)).thenReturn(true);
        when(orderItemRepository.existsByItemId(5L)).thenReturn(false);

        itemService.deleteItem(5L);

        verify(itemRepository).deleteById(5L);
    }

    @Test
    void deleteItem_unknownIdIsNotFound() {
        when(itemRepository.existsById(5L)).thenReturn(false);

        assertCode(catchThrowable(() -> itemService.deleteItem(5L)), NotFoundException.class, "item.not.found");
    }
}
