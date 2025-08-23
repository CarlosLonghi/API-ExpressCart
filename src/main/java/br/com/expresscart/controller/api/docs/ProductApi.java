package br.com.expresscart.controller.api.docs;

import br.com.expresscart.client.response.PlatziProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
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
                                schema = @Schema(implementation = PlatziProductResponse.class)
                            )
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Erro interno da aplicação", content = @Content)
    })
    ResponseEntity<List<PlatziProductResponse>> getAllProducts();

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
                            schema = @Schema(implementation = PlatziProductResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content)
    })
    ResponseEntity<PlatziProductResponse> getProductById(
            @Parameter(in = ParameterIn.PATH, description = "ID do Produto", required = true)
            @PathVariable Long id
    );
}
