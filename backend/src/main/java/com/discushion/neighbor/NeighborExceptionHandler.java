package com.discushion.neighbor;

import com.discushion.identity.IdentityErrorResponse;
import java.util.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Order(Ordered.HIGHEST_PRECEDENCE) @RestControllerAdvice(assignableTypes=NeighborController.class)
final class NeighborExceptionHandler {
    @ExceptionHandler(NeighborFailure.class)
    ResponseEntity<IdentityErrorResponse> failure(NeighborFailure error) {
        return ResponseEntity.status(error.status).body(new IdentityErrorResponse(error.code,"조회 지역을 확인해 주세요.",
            List.of(Map.of("field","regionId","reason","지역을 확인해 주세요.")),UUID.randomUUID().toString()));
    }
}
