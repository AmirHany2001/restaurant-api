package restaurant.site.services.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import restaurant.site.dto.ItemDto;
import restaurant.site.entity.Category;
import restaurant.site.entity.Item;
import restaurant.site.mapper.ItemMapper;
import restaurant.site.exception.BusinessLogicException;
import restaurant.site.exception.DuplicateResourceException;
import restaurant.site.exception.NotFoundException;
import restaurant.site.repo.CategoryRepository;
import restaurant.site.repo.ItemRepository;
import restaurant.site.repo.OrderItemRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemMapper itemMapper;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "items", allEntries = true),
            @CacheEvict(value = "category", allEntries = true) // category responses contain their items
    })
    public ItemDto.Response addItem(ItemDto.adminRequest request){

        if(itemRepository.existsByNameIgnoreCase(request.getName())){
            throw new DuplicateResourceException("item.duplicate.name", request.getName());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("category.not.found"));

        Item item = itemMapper.toEntity(request);

        item.setCategory(category); // the owning side: this is what writes category_id

        return itemMapper.toResponse(itemRepository.save(item));

    }


    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "items", allEntries = true),
            @CacheEvict(value = "category", allEntries = true) // category responses contain items
    })
    public void changePrice (long id , Map<Item.ItemSize, BigDecimal> newPrice){
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("item.not.found", id));

        if (newPrice == null || newPrice.isEmpty()) {
            throw new BusinessLogicException("item.price.required");
        }

        // always 2 decimals, so 10 and 10.00 count as the same price
        Map<Item.ItemSize, BigDecimal> price = new EnumMap<>(Item.ItemSize.class);
        for (Map.Entry<Item.ItemSize, BigDecimal> entry : newPrice.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().signum() <= 0) {
                throw new BusinessLogicException("item.price.min");
            }
            price.put(entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP));
        }

        if(Objects.equals(price, item.getPrice())){
            throw new BusinessLogicException("item.price.duplicate");
        }

        item.setPrice(price);
    }



    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "items", allEntries = true),
            @CacheEvict(value = "category", allEntries = true)
    })
    public void deleteItem(long id){

        if (!itemRepository.existsById(id)) {
            throw new NotFoundException("item.not.found", id);
        }

        // order history must keep pointing to a real item
        if (orderItemRepository.existsByItemId(id)) {
            throw new BusinessLogicException("item.delete.has.orders");
        }

        itemRepository.deleteById(id);

    }


    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "items", allEntries = true),
            @CacheEvict(value = "category", allEntries = true) // category responses contain items
    })
    public void changeState(long id , Item.Status status){

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("item.not.found", id));

        if(status == null || Objects.equals(status, item.getStatus())){
            throw new BusinessLogicException("item.status.duplicate");
        }

        item.setStatus(status);
    }


    @Transactional(readOnly = true)
    @Cacheable(value = "items", key = "#id")
    public ItemDto.Response getItemById(long id){
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("item.not.found", id));

        return itemMapper.toResponse(item);
    }


    @Transactional(readOnly = true)
    @Cacheable(value = "items", key = "'all'")
    public List<ItemDto.Response> getItems(){
        return itemRepository.findAll()
                .stream()
                .map(itemMapper::toResponse)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    @Cacheable(value = "items", key = "#status")
    public List<ItemDto.Response> getItemsByStatus(Item.Status status){
        return itemRepository.findByStatus(status)
                .stream()
                .map(itemMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "items", key = "'itemId: ' + #id +' '+'itemSize: ' + #size")
    public BigDecimal getItemPriceBySize(long id , Item.ItemSize size){
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("item.not.found", id));

        BigDecimal price = item.getPrice().get(size);
        if (price == null) { // the cache does not accept null values
            throw new BusinessLogicException("order.size.available");
        }
        return price;
    }

}
