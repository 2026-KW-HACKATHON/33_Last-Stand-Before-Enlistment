package com.discushion.signup;

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
@RestControllerAdvice(assignableTypes=SignupController.class)
final class SignupExceptionHandler {
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ResponseEntity<IdentityErrorResponse> unreadable() { return failure(SignupFailure.invalid("body")); }
    @ExceptionHandler(SignupFailure.class)
    ResponseEntity<IdentityErrorResponse> failure(SignupFailure error) {
        return ResponseEntity.status(error.reason.status).body(new IdentityErrorResponse(error.reason.name(),
            "가입 정보를 확인해 주세요.",List.of(Map.of("field",error.field,"reason","입력값을 확인해 주세요.")),UUID.randomUUID().toString()));
    }
}
