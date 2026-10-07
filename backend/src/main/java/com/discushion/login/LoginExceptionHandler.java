package com.discushion.login;

import com.discushion.identity.IdentityErrorResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes=LoginController.class)
final class LoginExceptionHandler {
    @ExceptionHandler({LoginInputFailure.class,HttpMessageNotReadableException.class})
    ResponseEntity<IdentityErrorResponse> invalid() {
        return ResponseEntity.badRequest().body(new IdentityErrorResponse("VALIDATION_ERROR",
            "빈 JSON 객체로 요청해 주세요.",List.of(Map.of("field","body","reason","빈 JSON 객체가 필요합니다.")),
            UUID.randomUUID().toString()));
    }
}
