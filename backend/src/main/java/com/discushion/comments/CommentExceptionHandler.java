package com.discushion.comments;

import com.discushion.identity.IdentityErrorResponse;
import java.util.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

@Order(Ordered.HIGHEST_PRECEDENCE) @RestControllerAdvice(assignableTypes = CommentController.class)
final class CommentExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<IdentityErrorResponse> invalid() { return failure(CommentFailure.invalid("body")); }
    @ExceptionHandler(CommentFailure.class)
    ResponseEntity<IdentityErrorResponse> failure(CommentFailure error) {
        String message = error.code.equals("COMMENT_FORBIDDEN_WORD") ? "금칙어가 포함되어 있습니다. 내용을 수정해 주세요." : "댓글과 요청을 확인해 주세요.";
        return ResponseEntity.status(error.status).body(new IdentityErrorResponse(error.code, message,
            error.status == 400 ? List.of(Map.of("field", error.field, "reason", message)) : List.of(), UUID.randomUUID().toString()));
    }
}
