package restaurant.site.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import restaurant.site.entity.Item;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {

    @NotBlank(message = "item.name.required")
    @Size(min = 3, max = 100, message = "item.name.size")
    private String itemName;

    @NotNull(message = "item.size.required")
    private Item.ItemSize size;

    @NotNull(message = "item.quantity.invalid")
    @Min(value = 1, message = "item.quantity.min")
    private Integer quantity;

    private String notes;

}
