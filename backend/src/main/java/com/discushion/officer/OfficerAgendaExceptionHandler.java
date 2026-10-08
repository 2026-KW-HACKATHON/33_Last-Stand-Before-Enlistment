package com.discushion.officer;

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
@RestControllerAdvice(assignableTypes = OfficerAgendaController.class)
final class OfficerAgendaExceptionHandler {
    @ExceptionHandler(OfficerAgendaFailure.class)
    ResponseEntity<IdentityErrorResponse> failure(OfficerAgendaFailure error) {
        return ResponseEntity.badRequest().body(new IdentityErrorResponse(error.code,
                "요청 값을 확인해 주세요.",
                List.of(Map.of("field", error.field, "reason", "입력값을 확인해 주세요.")),
                UUID.randomUUID().toString()));
    }
}
