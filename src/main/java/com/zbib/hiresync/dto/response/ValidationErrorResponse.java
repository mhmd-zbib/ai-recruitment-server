package com.zbib.hiresync.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Validation error response format.
 * Extends the standard error response with validation-specific details.
 * The validationErrors field is inherited from ErrorResponse.
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationErrorResponse extends ErrorResponse {
    
    public ValidationErrorResponse() {
        super();
    }
}
