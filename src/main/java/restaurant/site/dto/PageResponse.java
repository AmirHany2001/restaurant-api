package restaurant.site.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

// Our own page format (instead of Spring's Page): a small, stable JSON shape that can also be
// stored in and read back from Redis.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int page;          // page number, starts at 0
    private int size;          // requested page size
    private long totalElements;
    private int totalPages;

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
