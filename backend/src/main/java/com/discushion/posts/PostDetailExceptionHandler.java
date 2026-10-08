package com.discushion.posts;

import com.discushion.identity.IdentityErrorResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Order(-10)
@RestControllerAdvice
final class PostDetailExceptionHandler {
    @ExceptionHandler(PostDetailFailure.class)
    ResponseEntity<IdentityErrorResponse> failure(PostDetailFailure error) {
        return ResponseEntity.status(error.status).body(new IdentityErrorResponse(error.code,
                error.status == 404 ? "게시물을 찾을 수 없습니다." : "요청을 확인해 주세요.", List.of(), UUID.randomUUID().toString()));
    }
}
