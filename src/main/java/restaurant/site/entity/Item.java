package restaurant.site.entity;


import jakarta.persistence.*;
import lombok.*;

import java.util.Map;

@Entity
@Table(name = "Items")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    // size -> price for that size
    @ElementCollection
    @CollectionTable(name = "item_prices", joinColumns = @JoinColumn(name = "item_id"))
    @MapKeyColumn(name = "size")
    @Column(name = "price", nullable = false)
    private Map<ItemSize, Double> price;


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
