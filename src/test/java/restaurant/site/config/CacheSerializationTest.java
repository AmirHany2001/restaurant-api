package restaurant.site.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import restaurant.site.dto.ItemDto;
import restaurant.site.dto.PageResponse;
import restaurant.site.entity.Item;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

// No Redis needed: this only checks that what we cache can be written and read back
// with the serializer the real cache uses.
class CacheSerializationTest {

    private final GenericJacksonJsonRedisSerializer serializer = CacheConfig.jsonSerializer();

    @Test
    void pageOfItems_survivesTheRoundTrip() {
        ItemDto.Response item = new ItemDto.Response(
                7L, "Pizza", Map.of(Item.ItemSize.small, new BigDecimal("50.00")),
                "Cheese pizza", Item.Status.available, "Food");
        PageResponse<ItemDto.Response> page = new PageResponse<>(List.of(item), 0, 20, 1, 1);

        Object back = serializer.deserialize(serializer.serialize(page));

        PageResponse<?> result = assertInstanceOf(PageResponse.class, back);
        assertEquals(1, result.getTotalElements());
        ItemDto.Response first = assertInstanceOf(ItemDto.Response.class, result.getContent().get(0));
        assertEquals("Pizza", first.getName());
        assertEquals(new BigDecimal("50.00"), first.getPrice().get(Item.ItemSize.small));
        assertEquals(Item.Status.available, first.getStatus());
    }

    @Test
    void price_survivesTheRoundTrip() {
        Object back = serializer.deserialize(serializer.serialize(new BigDecimal("12.50")));

        assertEquals(new BigDecimal("12.50"), back);
    }
}
