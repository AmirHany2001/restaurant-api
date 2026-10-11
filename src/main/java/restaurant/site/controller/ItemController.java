package restaurant.site.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import restaurant.site.dto.ItemDto;
import restaurant.site.entity.Item;
import restaurant.site.services.application.ItemService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@NullMarked
public class ItemController {
    private final ItemService itemService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/createItem")
    public ResponseEntity<ItemDto.Response> addItem(@Valid @RequestBody ItemDto.adminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.addItem(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/changePrice/{id}")
    public ResponseEntity<Void> changePrice(@PathVariable long id,
                                            @RequestBody Map<Item.ItemSize, Double> price) {
        itemService.changePrice(id, price);
        return ResponseEntity.accepted().eTag("item.price.change").build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/deleteItem/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable long id) {
        itemService.deleteItem(id);
        return ResponseEntity.accepted().eTag("item.deleted.successfully").build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/changeState/{id}/{status}")
    public ResponseEntity<Void> changeState(@PathVariable long id, @PathVariable Item.Status status) {
        itemService.changeState(id, status);
        return ResponseEntity.accepted().eTag("item.status.change").build();
    }

    @GetMapping("/getItemById/{id}")
    public  ResponseEntity<ItemDto.Response> getItemById(@PathVariable long id) {
        return  ResponseEntity.ok(itemService.getItemById(id));
    }

    @GetMapping("/getAllItem")
    public ResponseEntity<List<ItemDto.Response>> getItems() {
        return ResponseEntity.ok(itemService.getItems());
    }

    @GetMapping("/getItemByStatus/{status}")
    public ResponseEntity<List<ItemDto.Response>> getItemsByStatus(@PathVariable Item.Status status) {
        return ResponseEntity.ok(itemService.getItemsByStatus(status));
    }

    @GetMapping("/getItemPriceBySize/{id}/{size}")
    public ResponseEntity<Double> getItemPriceBySize(@PathVariable long id,@Valid @PathVariable Item.ItemSize size) {
        return ResponseEntity.ok(itemService.getItemPriceBySize(id, size));
    }

}
