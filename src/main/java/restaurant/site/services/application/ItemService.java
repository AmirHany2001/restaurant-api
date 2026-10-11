package restaurant.site.services.application;

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
            throw new DuplicateResourceException("item.duplicate.name");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("category.not.found"));

        Item item = itemMapper.toEntity(request);

        item.setCategory(category); // the owning side: this is what writes category_id

        return itemMapper.toResponse(itemRepository.save(item));

    }


    @Transactional
    @CacheEvict(value = "items", allEntries = true)
    public void changePrice (long id , Map<Item.ItemSize, Double> price){
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("item.not.found"));

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
            throw new NotFoundException("item.not.found");
        }

        // order history must keep pointing to a real item
        if (orderItemRepository.existsByItemId(id)) {
            throw new BusinessLogicException("item.delete.has.orders");
        }

        itemRepository.deleteById(id);

    }


    @Transactional
    @CacheEvict(value = "items", allEntries = true)
    public void changeState(long id , Item.Status status){

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("item.not.found"));

        if(status == null || Objects.equals(status, item.getStatus())){
            throw new BusinessLogicException("item.status.duplicate");
        }

        item.setStatus(status);
    }


    @Cacheable(value = "items", key = "#id")
    public ItemDto.Response getItemById(long id){
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("item.not.found"));

        return itemMapper.toResponse(item);
    }


    @Cacheable(value = "items", key = "'all'")
    public List<ItemDto.Response> getItems(){
        return itemRepository.findAll()
                .stream()
                .map(itemMapper::toResponse)
                .collect(Collectors.toList());
    }


    @Cacheable(value = "items", key = "#status")
    public List<ItemDto.Response> getItemsByStatus(Item.Status status){
        return itemRepository.findByStatus(status)
                .stream()
                .map(itemMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "items", key = "'itemId: ' + #id +' '+'itemSize: ' + #size")
    public Double getItemPriceBySize(long id , Item.ItemSize size){
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("item.not.found"));
        return item.getPrice().get(size);
    }

}
