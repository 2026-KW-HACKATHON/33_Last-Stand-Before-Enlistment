package com.discushion.posts;

import com.discushion.identity.IdentityErrorResponse;
import com.discushion.identity.IdentityFailure;
import com.discushion.photos.PhotoFailure;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = PostController.class)
final class PostExceptionHandler {
    @ExceptionHandler(PostEditFailure.class)
    ResponseEntity<IdentityErrorResponse> failure(PostEditFailure error) {
        var response = new IdentityErrorResponse(error.code, "게시물과 요청 내용을 확인해 주세요.",
                List.of(Map.of("field", error.field)), UUID.randomUUID().toString());
        return ResponseEntity.status(error.status).body(response);
    }

    @ExceptionHandler(IdentityFailure.class)
    ResponseEntity<IdentityErrorResponse> identity(IdentityFailure failure) {
        var response = ResponseEntity.status(IdentityErrorResponse.status(failure));
        if (failure.reason() == IdentityFailure.Reason.INVALID_TOKEN) response.header("WWW-Authenticate", "Bearer");
        return response.body(IdentityErrorResponse.from(failure));
    }

    @ExceptionHandler(PhotoFailure.class)
    ResponseEntity<IdentityErrorResponse> photo(PhotoFailure failure) {
        var response = ResponseEntity.status(failure.reason().status);
        if (failure.retryAfter() != null) response.header("Retry-After", Long.toString(failure.retryAfter()));
        return response.body(new IdentityErrorResponse(failure.reason().name(), "사진 요청을 처리할 수 없습니다.",
                List.of(), UUID.randomUUID().toString()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<IdentityErrorResponse> internal(Exception ignored) {
        return ResponseEntity.status(500).body(IdentityErrorResponse.internal());
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ResponseEntity<IdentityErrorResponse> unreadable() {
        return ResponseEntity.badRequest().body(new IdentityErrorResponse("VALIDATION_ERROR",
                "요청을 확인해 주세요.", List.of(), UUID.randomUUID().toString()));
    }
}
