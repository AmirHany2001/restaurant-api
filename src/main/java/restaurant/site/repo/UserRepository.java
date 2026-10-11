package restaurant.site.repo;


import restaurant.site.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Check if email already exists — used before saving new user
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    // Find user by email — used in login
    Optional<User> findByEmailIgnoreCase(String email);



}
