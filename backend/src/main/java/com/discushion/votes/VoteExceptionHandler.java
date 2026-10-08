package com.discushion.votes;

import com.discushion.identity.IdentityErrorResponse;
import com.discushion.identity.IdentityFailure;
import java.util.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

@Order(Ordered.HIGHEST_PRECEDENCE) @RestControllerAdvice(assignableTypes=VoteController.class)
final class VoteExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<IdentityErrorResponse> unreadable(){return failure(VoteFailure.invalid("body"));}
    @ExceptionHandler(VoteFailure.class)
    ResponseEntity<IdentityErrorResponse> failure(VoteFailure error) {
        String message=switch(error.code){
            case "VOTE_CHANGE_CONFIRMATION_REQUIRED" -> "기존 선택을 변경하려면 확인해 주세요.";
            case "VOTE_ENDED" -> "종료된 투표에는 참여할 수 없습니다.";
            case "VOTE_OPTION_INVALID" -> "투표 선택지를 확인해 주세요.";
            default -> "투표와 요청을 확인해 주세요.";
        };
        return ResponseEntity.status(error.status).body(new IdentityErrorResponse(error.code,message,
            error.status==400?List.of(Map.of("field",error.field,"reason",message)):List.of(),UUID.randomUUID().toString()));
    }
    @ExceptionHandler(IdentityFailure.class)
    ResponseEntity<IdentityErrorResponse> identity(IdentityFailure error) {
        var response=ResponseEntity.status(IdentityErrorResponse.status(error));
        if(error.reason()==IdentityFailure.Reason.INVALID_TOKEN)response.header("WWW-Authenticate","Bearer");
        return response.body(IdentityErrorResponse.from(error));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<IdentityErrorResponse> internal(Exception ignored){return ResponseEntity.status(500).body(IdentityErrorResponse.internal());}
}
