package restaurant.site.repo;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import restaurant.site.entity.Item;

import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {


    Page<Item> findByStatus(Item.Status status, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCategoryId(Long categoryId);

    Optional<Item> findByNameIgnoreCase(String name);


}
