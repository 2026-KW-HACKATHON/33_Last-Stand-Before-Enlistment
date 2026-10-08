package com.discushion.profile;

import com.discushion.identity.IdentityErrorResponse;
import java.util.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

@Order(Ordered.HIGHEST_PRECEDENCE) @RestControllerAdvice(assignableTypes=ProfileController.class)
final class ProfileExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<IdentityErrorResponse> invalid() {return failure(ProfileFailure.invalid("body"));}
    @ExceptionHandler(ProfileFailure.class)
    ResponseEntity<IdentityErrorResponse> failure(ProfileFailure error) {
        return ResponseEntity.status(error.status).body(new IdentityErrorResponse(error.code,"프로필 정보를 확인해 주세요.",
            List.of(Map.of("field",error.field,"reason","입력값을 확인해 주세요.")),UUID.randomUUID().toString()));
    }
}
