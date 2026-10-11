package restaurant.site.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import restaurant.site.entity.Item;

import java.util.Map;

public class ItemDto {


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class adminRequest{

        @NotBlank(message = "item.name.required")
        @Size(min = 3, max = 100, message = "item.name.size")
        private String name;

        @NotBlank(message = "item.description.required")
        @Size(min = 3, max = 100, message = "item.description.size")
        private String description;

        // size -> price for that size
        @NotEmpty(message = "error.invalid.parameter")
        @Positive(message = "item.price.min")
        private Map<Item.ItemSize, Double> price;

        @NotNull(message = "category.id.required")
        private Long categoryId;

        @NotNull(message = "item.status.required")
        private Item.Status status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response{
        private long id;
        private String name;
        private Map<Item.ItemSize, Double> price;
        private String description;
        private Item.Status status;
        private String categoryName;
    }

}
