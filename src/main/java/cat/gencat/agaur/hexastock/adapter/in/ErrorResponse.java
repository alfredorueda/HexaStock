package cat.gencat.agaur.hexastock.adapter.in;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * REST API error response class
 */
@Schema(description = "Error response")
public class ErrorResponse {
    
    @Schema(description = "HTTP status code", example = "404")
    private final int status;
    
    @Schema(description = "Error message", example = "Stock not found for symbol: XYZ")
    private final String message;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
