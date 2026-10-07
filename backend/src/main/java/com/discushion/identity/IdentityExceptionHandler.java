package com.discushion.identity;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class IdentityExceptionHandler {
    @ExceptionHandler(IdentityFailure.class)
    public ResponseEntity<IdentityErrorResponse> handle(IdentityFailure failure) {
        var response = ResponseEntity.status(IdentityErrorResponse.status(failure));
        if (failure.reason() == IdentityFailure.Reason.INVALID_TOKEN) response.header("WWW-Authenticate", "Bearer");
        return response.body(IdentityErrorResponse.from(failure));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<IdentityErrorResponse> internal(Exception error) {
        if (error instanceof org.springframework.web.ErrorResponse frameworkError) {
            var status = frameworkError.getStatusCode();
            if (status.is4xxClientError()) {
                return ResponseEntity.status(status).body(new IdentityErrorResponse("VALIDATION_ERROR",
                    "요청을 확인해 주세요.", java.util.List.of(), java.util.UUID.randomUUID().toString()));
            }
        }
        return ResponseEntity.status(500).body(IdentityErrorResponse.internal());
    }
}
