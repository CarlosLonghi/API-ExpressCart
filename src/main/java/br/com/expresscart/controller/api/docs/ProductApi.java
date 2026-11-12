package br.com.expresscart.controller.api.docs;

import br.com.expresscart.controller.response.ProductResponse;
import br.com.expresscart.exception.response.GenericErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(
        name = "Produto",
        description = "Operações para listar os produtos da API externa"
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
@ApiResponse(responseCode = "502", description = "Falha ao consultar a API externa de produtos (Platzi)",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = GenericErrorResponse.class),
                examples = @ExampleObject(
                    name = "API externa indisponível",
                    value = """
                        {
                          "timestamp": "2025-08-27T01:50:59.386Z",
                          "status": 502,
                          "message": "Erro ao consultar o catálogo de produtos (Platzi)"
                        }
                    """
                )
        )
)
public interface ProductApi {

    @Operation(
            summary = "Buscar produtos da API externa",
            description = "Rota para listar produtos da API externa e salvar em cache (Redis)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de produtos retornada com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema (
                                schema = @Schema(implementation = ProductResponse.class)
                            ),
                            examples = @ExampleObject(
                                name = "Lista de Produtos",
                                value = """
                                    [
                                      {
                                        "id": 103,
                                        "title": "Demo Product",
                                        "price": 10,
                                        "images": [
                                          "https://placehold.co/600x400"
                                        ],
                                        "category": {
                                          "id": 1,
                                          "name": "Demo Category",
                                          "image": "https://placeimg.com/640/480/any"
                                        }
                                      }
                                    ]
                                """
                            )
                    )
            )
    })
    ResponseEntity<List<ProductResponse>> getAllProducts();

    @Operation(
            summary = "Buscar produto por ID",
            description = "Rota para buscar produto por ID do cache (Redis)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Produto retornado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProductResponse.class),
                            examples = @ExampleObject(
                                name = "Dados do Produto",
                                value = """
                                  {
                                    "id": 103,
                                    "title": "Demo Product",
                                    "price": 10,
                                    "images": [
                                      "https://placehold.co/600x400"
                                    ],
                                    "category": {
                                      "id": 1,
                                      "name": "Demo Category",
                                      "image": "https://placeimg.com/640/480/any"
                                    }
                                  }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GenericErrorResponse.class),
                            examples = @ExampleObject(
                                name = "Produto não encontrado",
                                value = """
                                    {
                                      "timestamp": "2025-08-27T01:50:59.386Z",
                                      "status": 404,
                                      "message": "Produto não encontrado: id=1"
                                    }
                                """
                            )
                    )
            )
    })
    ResponseEntity<ProductResponse> getProductById(
            @Parameter(in = ParameterIn.PATH, description = "ID do Produto", required = true)
            @PathVariable Long id
    );
}
