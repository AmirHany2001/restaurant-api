package restaurant.site.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PagingTest {

    @Test
    void aNegativePageBecomesTheFirstPage() {
        assertThat(Paging.page(-3)).isZero();
        assertThat(Paging.page(4)).isEqualTo(4);
    }

    @Test
    void theSizeStaysBetweenOneAndTheMaximum() {
        assertThat(Paging.size(0)).isEqualTo(1);
        assertThat(Paging.size(-10)).isEqualTo(1);
        assertThat(Paging.size(20)).isEqualTo(20);
        assertThat(Paging.size(1_000_000)).isEqualTo(Paging.MAX_SIZE); // also keeps the number of cache keys small
    }
}
