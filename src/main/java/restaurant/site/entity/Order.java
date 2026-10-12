package restaurant.site.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor // prevents infinite loop when printing
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Order.Status status = Status.pending;

    public enum Status {
        pending, approved, completed, cancelled;

        // allowed moves: pending -> approved | cancelled, approved -> completed
        // completed and cancelled are final
        public boolean canChangeTo(Status next) {
            return switch (this) {
                case pending -> next == approved || next == cancelled;
                case approved -> next == completed;
                case completed, cancelled -> false;
            };
        }
    }
}
