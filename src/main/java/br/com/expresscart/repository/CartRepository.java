package br.com.expresscart.repository;

import br.com.expresscart.entity.Cart;
import br.com.expresscart.entity.Status;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CartRepository extends MongoRepository<Cart, String> {
    Optional<Cart> findByClientIdAndStatus(Long clientId, Status status);
}
