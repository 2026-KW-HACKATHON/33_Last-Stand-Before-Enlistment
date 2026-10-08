package com.discushion.evaluations;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController @Profile("!local")
final class EvaluationController {
    private final EvaluationService service;
    EvaluationController(EvaluationService service){this.service=service;}
    @PutMapping("/api/v1/comments/{commentId}/evaluation")
    Map<String,Object> select(@PathVariable("commentId")String id,@RequestBody Map<String,Object> body) {
        var input=EvaluationInput.select(id,body);return service.set(input.commentId(),input.type());
    }
    @DeleteMapping("/api/v1/comments/{commentId}/evaluation")
    Map<String,Object> cancel(@PathVariable("commentId")String id,@RequestBody(required=false)String body) {
        if(body!=null && !body.isBlank())throw EvaluationFailure.invalid("body");return service.set(EvaluationInput.pathId(id),null);
    }
}
