package br.com.expresscart.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class Product {

    private String id;
    private String title;
    private BigDecimal price;
    private Integer quantity;
}
