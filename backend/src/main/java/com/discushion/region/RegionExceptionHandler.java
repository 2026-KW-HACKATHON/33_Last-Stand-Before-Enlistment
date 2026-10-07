package com.discushion.region;

import com.discushion.identity.IdentityErrorResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = RegionController.class)
public final class RegionExceptionHandler {
    @ExceptionHandler(RegionInputFailure.class)
    public ResponseEntity<IdentityErrorResponse> invalid(RegionInputFailure failure) {
        return ResponseEntity.badRequest().body(new IdentityErrorResponse("VALIDATION_ERROR",
            failure.getMessage(), List.of(Map.of("field", failure.field(), "reason", "입력값을 확인해 주세요.")),
            UUID.randomUUID().toString()));
    }
}
