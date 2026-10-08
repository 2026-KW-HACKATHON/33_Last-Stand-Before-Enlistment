package com.discushion.personal;

import com.discushion.identity.IdentityErrorResponse;
import java.util.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {PersonalController.class,BoardController.class})
final class PersonalExceptionHandler {
    @ExceptionHandler(BoardFailure.class)
    ResponseEntity<IdentityErrorResponse> region(BoardFailure failure){
        return ResponseEntity.status(404).body(new IdentityErrorResponse("REGION_NOT_FOUND","지역을 찾을 수 없습니다.",List.of(),UUID.randomUUID().toString()));
    }
    @ExceptionHandler(PersonalFailure.class)
    ResponseEntity<IdentityErrorResponse> invalid(PersonalFailure failure) {
        return ResponseEntity.badRequest().body(new IdentityErrorResponse("VALIDATION_ERROR", "조회 조건을 확인해 주세요.",
                List.of(Map.of("field", failure.field, "reason", "허용된 조회 조건이 아닙니다.")), UUID.randomUUID().toString()));
    }
}
