package com.discushion.share;

import com.discushion.identity.IdentityErrorResponse;
import java.util.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Order(Ordered.HIGHEST_PRECEDENCE) @RestControllerAdvice
final class ShareExceptionHandler {
    @ExceptionHandler(ShareFailure.class)
    ResponseEntity<IdentityErrorResponse> failure(ShareFailure failure) {
        String message = failure.status == 404 ? "게시물을 찾을 수 없습니다." : "공유 링크와 요청을 확인해 주세요.";
        var response = ResponseEntity.status(failure.status);
        if (failure.status == 401) response.header("WWW-Authenticate", "Bearer");
        return response.body(new IdentityErrorResponse(failure.code, message, List.of(), UUID.randomUUID().toString()));
    }
}
