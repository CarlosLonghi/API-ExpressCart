package br.com.expresscart.entity;

import br.com.expresscart.exception.BusinessException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Carrinho de compras. Todas as mudanças de estado passam pelos métodos desta classe,
 * que garantem as regras: carrinho vendido não muda mais e o total sempre reflete os itens.
 */
@Document(collection = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // usado pelo Spring Data ao ler do MongoDB
public class Cart {

    private String id;

    private Long clientId;

    private BigDecimal totalPrice;

    private List<Product> products;

    private Status status;

    private PaymentMethod paymentMethod;

    private Cart(Long clientId, List<Product> products) {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.status = Status.OPEN;
        setItems(products);
    }

    /** Abre um carrinho novo para o cliente, já com os itens e o total calculado. */
    public static Cart open(Long clientId, List<Product> products) {
        return new Cart(clientId, products);
    }

    /**
     * Lança {@link BusinessException} se os itens não puderem mais ser alterados.
     * Permite recusar a operação antes de consultar o catálogo externo.
     */
    public void ensureItemsCanChange() {
        ensureOpen("Não é possivel alterar os dados de um carrinho já vendido.");
    }

    /** Substitui todos os itens do carrinho. */
    public void replaceItems(List<Product> products) {
        ensureItemsCanChange();
        setItems(products);
    }

    /** Remove todos os itens do carrinho. */
    public void clear() {
        ensureOpen("Não é possivel limpar um carrinho já vendido.");
        setItems(List.of());
    }

    /** Registra o pagamento e encerra o carrinho. */
    public void pay(PaymentMethod paymentMethod) {
        ensureOpen("Operação cancelada. O carrinho informado já foi pago.");
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "paymentMethod");
        this.status = Status.SOLD;
    }

    public boolean isSold() {
        return status == Status.SOLD;
    }

    private void ensureOpen(String message) {
        if (isSold()) {
            throw new BusinessException(message);
        }
    }

    private void setItems(List<Product> products) {
        this.products = List.copyOf(products);
        this.totalPrice = this.products.stream()
                .map(Product::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
