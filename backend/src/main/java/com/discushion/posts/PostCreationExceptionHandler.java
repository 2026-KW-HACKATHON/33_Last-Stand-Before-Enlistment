package com.discushion.posts;

import com.discushion.identity.IdentityErrorResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@org.springframework.core.annotation.Order(-10)
@RestControllerAdvice(assignableTypes = PostCreationController.class)
final class PostCreationExceptionHandler {
    @ExceptionHandler(PostCreationFailure.class)
    ResponseEntity<IdentityErrorResponse> invalid(PostCreationFailure failure) {
        return ResponseEntity.badRequest().body(new IdentityErrorResponse("VALIDATION_ERROR",
                "요청을 확인해 주세요.", List.of(), UUID.randomUUID().toString()));
    }
}
