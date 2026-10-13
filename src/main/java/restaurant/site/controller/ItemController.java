package restaurant.site.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import restaurant.site.dto.ItemDto;
import restaurant.site.dto.MessageResponse;
import restaurant.site.entity.Item;
import restaurant.site.services.application.ItemService;
import restaurant.site.services.language.ChooseLanguageService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@NullMarked
public class ItemController {
    private final ItemService itemService;
    private final ChooseLanguageService chooseLanguageService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ItemDto.Response> addItem(@Valid @RequestBody ItemDto.adminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.addItem(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/price")
    public ResponseEntity<MessageResponse> changePrice(@PathVariable long id,
                                                       @RequestBody Map<Item.ItemSize, BigDecimal> price) {
        itemService.changePrice(id, price);
        return ResponseEntity.ok(new MessageResponse(
                chooseLanguageService.getAcceptedLanguageMessage("item.price.change")));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteItem(@PathVariable long id) {
        itemService.deleteItem(id);
        return ResponseEntity.ok(new MessageResponse(
                chooseLanguageService.getAcceptedLanguageMessage("item.deleted.successfully")));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status/{status}")
    public ResponseEntity<MessageResponse> changeState(@PathVariable long id, @PathVariable Item.Status status) {
        itemService.changeState(id, status);
        return ResponseEntity.ok(new MessageResponse(
                chooseLanguageService.getAcceptedLanguageMessage("item.status.change")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemDto.Response> getItemById(@PathVariable long id) {
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    // GET /items                    -> all items
    // GET /items?status=available   -> only the items with that status
    @GetMapping
    public ResponseEntity<List<ItemDto.Response>> getItems(@RequestParam(required = false) Item.@Nullable Status status) {
        return ResponseEntity.ok(status == null ? itemService.getItems() : itemService.getItemsByStatus(status));
    }

    @GetMapping("/{id}/price/{size}")
    public ResponseEntity<BigDecimal> getItemPriceBySize(@PathVariable long id, @PathVariable Item.ItemSize size) {
        return ResponseEntity.ok(itemService.getItemPriceBySize(id, size));
    }

}
