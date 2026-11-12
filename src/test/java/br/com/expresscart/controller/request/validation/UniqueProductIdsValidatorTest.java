package br.com.expresscart.controller.request.validation;

import br.com.expresscart.controller.request.ProductRequest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static br.com.expresscart.support.TestFixtures.item;
import static org.assertj.core.api.Assertions.assertThat;

class UniqueProductIdsValidatorTest {

    private final UniqueProductIdsValidator validator = new UniqueProductIdsValidator();

    private boolean isValid(List<ProductRequest> products) {
        return validator.isValid(products, null);
    }

    @Test
    void acceptsDistinctIds() {
        assertThat(isValid(List.of(item(1L, 1), item(2L, 1), item(3L, 5)))).isTrue();
    }

    @Test
    void rejectsRepeatedId() {
        assertThat(isValid(List.of(item(1L, 1), item(2L, 1), item(1L, 3)))).isFalse();
    }

    @Test
    void acceptsEmptyAndNullLists() {
        // lista nula é recusada pelo @NotNull, não por esta regra
        assertThat(isValid(List.of())).isTrue();
        assertThat(isValid(null)).isTrue();
    }

    @Test
    void ignoresItemsWithoutIdAndNullItems() {
        // item sem id é recusado pelo @NotNull do ProductRequest, não por esta regra
        assertThat(isValid(Arrays.asList(item(null, 1), item(null, 2), null, null, item(1L, 1)))).isTrue();
    }
}
