package br.com.expresscart.controller.request.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida que uma lista de produtos não traz o mesmo id de produto mais de uma vez.
 * Lista nula e itens sem id são considerados válidos aqui: quem os recusa é o {@code @NotNull}.
 */
@Documented
@Constraint(validatedBy = UniqueProductIdsValidator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueProductIds {

    String message() default "A lista não pode conter o mesmo produto mais de uma vez";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
