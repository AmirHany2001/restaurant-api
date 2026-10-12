package restaurant.site.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
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

    // only sent back to the client; the price is always taken from the database
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal unitPriceAtOrder;

}
