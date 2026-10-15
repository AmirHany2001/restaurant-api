package restaurant.site.entity;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    // The complete table of allowed moves. Everything not listed here must be refused,
    // so a new status or a reordered enum cannot silently allow a wrong move.
    private static final Set<String> ALLOWED = Set.of(
            "pending->approved",
            "pending->cancelled",
            "approved->completed");

    @Test
    void onlyTheDocumentedMovesAreAllowed() {
        for (Order.Status from : Order.Status.values()) {
            for (Order.Status to : Order.Status.values()) {
                assertThat(from.canChangeTo(to))
                        .as("%s -> %s", from, to)
                        .isEqualTo(ALLOWED.contains(from + "->" + to));
            }
        }
    }

    @Test
    void completedAndCancelledAreFinal() {
        for (Order.Status next : Order.Status.values()) {
            assertThat(Order.Status.completed.canChangeTo(next)).isFalse();
            assertThat(Order.Status.cancelled.canChangeTo(next)).isFalse();
        }
    }
}
