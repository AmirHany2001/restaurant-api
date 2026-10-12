package restaurant.site.repo;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import restaurant.site.entity.Category;
import restaurant.site.entity.Item;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {


    List<Item> findByStatus(Item.Status status);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCategoryId(Long categoryId);

    Optional<Item> findByNameIgnoreCase(String name);


}
