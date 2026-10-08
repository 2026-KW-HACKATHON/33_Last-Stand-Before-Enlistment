package com.discushion.summary;
import com.discushion.identity.IdentityErrorResponse;
import java.util.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@Order(Ordered.HIGHEST_PRECEDENCE) @RestControllerAdvice(assignableTypes=SummaryController.class)
final class SummaryExceptionHandler {
    @ExceptionHandler(SummaryFailure.class) ResponseEntity<IdentityErrorResponse> failure(SummaryFailure e){
        return ResponseEntity.status(e.status).body(new IdentityErrorResponse(e.code,
            e.status==404?"게시물을 찾을 수 없습니다.":e.status==422?"지역 안건만 요약할 수 있습니다.":"요청을 확인해 주세요.",
            List.of(),UUID.randomUUID().toString()));
    }
}
