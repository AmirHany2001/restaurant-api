package restaurant.site.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor // prevents infinite loop when printing
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;


    @Column(unique = true, nullable = false)
    private String email;


    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String address;

    @OneToMany(mappedBy = "user" , cascade = CascadeType.ALL)
    List<Order> orders ;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role ;

    public enum     Role {
        USER, ADMIN
    }
}


