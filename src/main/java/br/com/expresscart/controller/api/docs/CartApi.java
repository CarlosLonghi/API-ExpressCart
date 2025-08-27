package br.com.expresscart.controller.api.docs;

import br.com.expresscart.controller.request.CartRequest;
import br.com.expresscart.controller.request.PaymentRequest;
import br.com.expresscart.entity.Cart;
import br.com.expresscart.exception.response.GenericErrorResponse;
import br.com.expresscart.exception.response.ValidationErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
@ApiResponse(responseCode = "500", description = "Erro interno da aplicação",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = GenericErrorResponse.class),
                examples = @ExampleObject(
                    name = "Erro Interno",
                    value = """
                        {
                          "timestamp": "2025-08-27T01:50:59.386Z",
                          "status": 500,
                          "message": "Erro interno da aplicação"
                        }
                    """
                )
        )
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
            @ApiResponse(responseCode = "400", description = "Carrinho Duplicado / Erro de Validação",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    oneOf = {GenericErrorResponse.class, ValidationErrorResponse.class}
                            ),
                            examples = {
                                    @ExampleObject(
                                        name = "Carrinho duplicado",
                                        value = """
                                            {
                                              "timestamp": "2025-08-27T01:50:59.386Z",
                                              "status": 400,
                                              "message": "Já existe um Carrinho aberto para o cliente informado"
                                            }
                                        """
                                    ),
                                    @ExampleObject(
                                        name = "Erro de validação",
                                        value = """
                                            {
                                              "field": "email",
                                              "message": "O campo email deve ser um endereço válido"
                                            }
                                        """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado (Platzi Client)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GenericErrorResponse.class),
                            examples = @ExampleObject(
                                name = "Produto não encontrado",
                                value = """
                                    {
                                      "timestamp": "2025-08-27T01:50:59.386Z",
                                      "status": 404,
                                      "message": "Produto não encontrado (Platzi Client)"
                                    }
                                """
                            )
                    )
            )
    })
    ResponseEntity<Cart> createCart(
      @RequestBody(
              description = "Requisição para criar novo carrinho",
              required = true,
              content = @Content(
                      schema = @Schema(implementation = CartRequest.class),
                      examples = @ExampleObject(
                          name = "Criar Carrinho",
                          value = """
                              {
                                  "clientId": "1",
                                  "products": [
                                    {
                                      "id": 1,
                                      "quantity": 1
                                    }
                                  ]
                              }
                          """
                      )
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
            @ApiResponse(responseCode = "404", description = "Carrinho não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GenericErrorResponse.class),
                            examples = @ExampleObject(
                                name = "Carrinho não encontrado",
                                value = """
                                    {
                                      "timestamp": "2025-08-27T11:08:27.627557053",
                                      "status": 404,
                                      "message": "Carrinho não encontrado."
                                    }
                                """
                            )
                    )
            )
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
                            // TODO: adicionar example após criar o Response DTO
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Carrinho Duplicado / Erro de Validação",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    oneOf = {GenericErrorResponse.class, ValidationErrorResponse.class}
                            ),
                            examples = {
                                    @ExampleObject(
                                        name = "Carrinho duplicado",
                                        value = """
                                            {
                                              "timestamp": "2025-08-27T01:50:59.386Z",
                                              "status": 400,
                                              "message": "Já existe um Carrinho aberto para o cliente informado"
                                            }
                                        """
                                    ),
                                    @ExampleObject(
                                        name = "Erro de validação",
                                        value = """
                                            {
                                              "field": "email",
                                              "message": "O campo email deve ser um endereço válido"
                                            }
                                        """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado (Platzi Client)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GenericErrorResponse.class),
                            examples = @ExampleObject(
                                name = "Produto não encontrado",
                                value = """
                                    {
                                      "timestamp": "2025-08-27T01:50:59.386Z",
                                      "status": 404,
                                      "message": "Produto não encontrado (Platzi Client)"
                                    }
                                """
                            )
                    )
            )
    })
    ResponseEntity<Cart> updateCart(
            @Parameter(in = ParameterIn.PATH, description = "ID do Carrinho", required = true)
            @PathVariable String id,
            @RequestBody(
                    description = "Requisição para atualizar um carrinho",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CartRequest.class),
                            examples = @ExampleObject(
                                name = "Atualizar Carrinho",
                                value = """
                                    {
                                      "clientId": "1",
                                      "products": [
                                        {
                                          "id": 1,
                                          "quantity": 1
                                        }
                                      ]
                                    }
                                """
                            )
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
            @ApiResponse(responseCode = "400", description = "Operação cancelada. O carrinho informado já foi pago.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GenericErrorResponse.class),
                            examples = @ExampleObject(
                                name = "Pagar Carrinho vendido",
                                value = """
                                    {
                                      "timestamp": "2025-08-27T11:08:27.627557053",
                                      "status": 400,
                                      "message": "Operação cancelada. O carrinho informado já foi pago."
                                    }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Carrinho não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GenericErrorResponse.class),
                            examples = @ExampleObject(
                                name = "Carrinho não encontrado",
                                value = """
                                    {
                                      "timestamp": "2025-08-27T11:08:27.627557053",
                                      "status": 404,
                                      "message": "Carrinho não encontrado."
                                    }
                                """
                            )
                    )
            )
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
            @ApiResponse(responseCode = "400", description = "Não é possível limpar um carrinho já vendido.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GenericErrorResponse.class),
                            examples = @ExampleObject(
                                name = "Limpar Carrinho vendido",
                                value = """
                                    {
                                      "timestamp": "2025-08-27T11:08:27.627557053",
                                      "status": 400,
                                      "message": "Não é possível limpar um carrinho já vendido."
                                    }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Carrinho não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GenericErrorResponse.class),
                            examples = @ExampleObject(
                                name = "Carrinho não encontrado",
                                value = """
                                    {
                                      "timestamp": "2025-08-27T11:08:27.627557053",
                                      "status": 404,
                                      "message": "Carrinho não encontrado."
                                    }
                                """
                            )
                    )
            )
    })
    ResponseEntity<Cart> clearCart(
            @Parameter(in = ParameterIn.PATH, description = "ID do Carrinho", required = true)
            @PathVariable String id
    );
}
