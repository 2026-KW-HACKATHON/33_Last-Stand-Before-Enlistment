package com.discushion.reactions;

import com.discushion.identity.IdentityErrorResponse;
import java.util.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Order(Ordered.HIGHEST_PRECEDENCE) @RestControllerAdvice(assignableTypes=ReactionController.class)
final class ReactionExceptionHandler {
    @ExceptionHandler(ReactionFailure.class)
    ResponseEntity<IdentityErrorResponse> failure(ReactionFailure error) {
        return ResponseEntity.status(error.status).body(new IdentityErrorResponse(error.code,"반응과 요청을 확인해 주세요.",
            error.status==400?List.of(Map.of("field",error.field,"reason","입력값을 확인해 주세요.")):List.of(),UUID.randomUUID().toString()));
    }
}
