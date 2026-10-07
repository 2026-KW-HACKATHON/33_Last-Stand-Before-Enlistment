package com.discushion.photos;

import com.discushion.identity.IdentityErrorResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(-10)
@RestControllerAdvice
public final class PhotoExceptionHandler {
    @ExceptionHandler(PhotoFailure.class)
    ResponseEntity<IdentityErrorResponse> handle(PhotoFailure failure) {
        var response=ResponseEntity.status(failure.reason().status);
        if(failure.retryAfter()!=null) response.header("Retry-After",Long.toString(failure.retryAfter()));
        return response.body(new IdentityErrorResponse(failure.reason().name(),"사진 요청을 처리할 수 없습니다.",List.of(),UUID.randomUUID().toString()));
    }
}
