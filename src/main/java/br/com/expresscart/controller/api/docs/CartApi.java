package br.com.expresscart.controller.api.docs;

import br.com.expresscart.controller.request.CartRequest;
import br.com.expresscart.controller.request.PaymentRequest;
import br.com.expresscart.entity.Cart;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(
        name = "Carrinho",
        description = "Operações para gerenciar o carrinho de compras"
)
public interface CartApi {

    @Operation(
            summary = "Criar novo Carrinho",
            description = "Rota para criar um novo Carrinho com produtos"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Carrinho criado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Cart.class) // TODO: criar Response DTO para carrinho
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido", content = @Content),
            @ApiResponse(responseCode = "400", description = "Já existe um Carrinho aberto para o cliente informado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado (client error)", content = @Content)
    })
    ResponseEntity<Cart> createCart(
      @RequestBody(
              description = "Requisição para criar novo carrinho",
              required = true,
              content = @Content(
                      schema = @Schema(implementation = CartRequest.class)
              )
      )
      CartRequest request
    );

    @Operation(
            summary = "Buscar carrinho por ID",
            description = "Rota para mostrar os dados de um Carrinho"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Dados do Carrinho",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Cart.class) // TODO: criar Response DTO para carrinho
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Carrinho não encontrado", content = @Content)
    })
    ResponseEntity<Cart> getCartById(
            @Parameter(in = ParameterIn.PATH, description = "ID do Carrinho", required = true)
            @PathVariable String id
    );

    // TODO: criar Response DTO para atualizar carrinho, alterando apenas os itens do carrinho
    @Operation(
            summary = "Atualizar um Carrinho",
            description = "Rota para atualizar um Carrinho, adicionando ou removendo produtos"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Carrinho atualizado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Cart.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido", content = @Content),
            @ApiResponse(responseCode = "400", description = "Já existe um Carrinho aberto para o cliente informado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado (client error)", content = @Content)
    })
    ResponseEntity<Cart> updateCart(
            @Parameter(in = ParameterIn.PATH, description = "ID do Carrinho", required = true)
            @PathVariable String id,
            @RequestBody(
                    description = "Requisição para atualizar um carrinho",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CartRequest.class)
                    )
            )
            CartRequest request
    );

    @Operation(
            summary = "Pagar carrinho por ID",
            description = "Rota para realizar o pagamento de um carrinho"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Dados do Carrinho",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Cart.class) // TODO: criar Response DTO para carrinho
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Operação cancelada. O carrinho informado já foi pago.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Carrinho não encontrado", content = @Content)
    })
    ResponseEntity<Cart> payCart(
            @Parameter(in = ParameterIn.PATH, description = "ID do Carrinho", required = true)
            @PathVariable String id,
            @RequestBody(
                    description = "Requisição para pagar um carrinho",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = PaymentRequest.class)
                    )
            )
            PaymentRequest request
    );

    @Operation(
            summary = "Limpar Carrinho por ID",
            description = "Rota para limpar os items adicionados ao Carrinho"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrinho limpo", content = @Content),
            @ApiResponse(responseCode = "400", description = "Não é possível limpar um carrinho já vendido.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Carrinho não encontrado", content = @Content)
    })
    ResponseEntity<Cart> clearCart(
            @Parameter(in = ParameterIn.PATH, description = "ID do Carrinho", required = true)
            @PathVariable String id
    );
}
