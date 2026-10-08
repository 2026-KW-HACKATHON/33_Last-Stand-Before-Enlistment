package com.discushion.adoption;

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
@RestControllerAdvice(assignableTypes = AdoptionController.class)
final class AdoptionExceptionHandler {
    @ExceptionHandler(AdoptionFailure.class)
    ResponseEntity<IdentityErrorResponse> failure(AdoptionFailure error) {
        return ResponseEntity.status(error.status).body(new IdentityErrorResponse(error.code,
                error.status == 404 ? "요청한 안건 또는 채택을 찾을 수 없습니다." : "요청을 처리할 수 없습니다.",
                List.of(Map.of("field", error.field, "reason", "요청 값과 권한을 확인해 주세요.")),
                UUID.randomUUID().toString()));
    }
}
