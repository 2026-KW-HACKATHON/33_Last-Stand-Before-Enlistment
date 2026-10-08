package com.discushion.map;
import com.discushion.identity.IdentityErrorResponse;
import java.util.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@Order(Ordered.HIGHEST_PRECEDENCE) @RestControllerAdvice(assignableTypes=DongController.class)
final class MapExceptionHandler {
    @ExceptionHandler(MapFailure.class) ResponseEntity<IdentityErrorResponse> failure(MapFailure e){
        return ResponseEntity.status(e.status).body(new IdentityErrorResponse(e.code,
            e.status==404?"지역을 찾을 수 없습니다.":"입력값을 확인해 주세요.",
            e.field==null?List.of():List.of(Map.of("field",e.field,"reason","입력값을 확인해 주세요.")),UUID.randomUUID().toString()));
    }
}
