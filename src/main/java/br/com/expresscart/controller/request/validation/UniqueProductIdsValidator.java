package br.com.expresscart.controller.request.validation;

import br.com.expresscart.controller.request.ProductRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class UniqueProductIdsValidator implements ConstraintValidator<UniqueProductIds, List<ProductRequest>> {

    @Override
    public boolean isValid(List<ProductRequest> products, ConstraintValidatorContext context) {
        if (products == null) {
            return true;
        }

        Set<Long> seen = new HashSet<>();
        return products.stream()
                .filter(Objects::nonNull)
                .map(ProductRequest::id)
                .filter(Objects::nonNull)
                .allMatch(seen::add);
    }
}
