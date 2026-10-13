package restaurant.site.entity;


import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

import java.util.Map;

@Entity
@Table(name = "items")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique: the service checks duplicates, but two requests at the same moment can both pass
    // that check. Only a database constraint stops them (the error is then handled as a 409).
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String description;

    // size -> price for that size
    @ElementCollection
    @CollectionTable(name = "item_prices", joinColumns = @JoinColumn(name = "item_id"))
    @MapKeyColumn(name = "size")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private Map<ItemSize, BigDecimal> price;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Item.Status status = Status.not_available;

    public enum Status {
        available , not_available
    }

    public enum ItemSize {
        small , medium , large
    }

}
