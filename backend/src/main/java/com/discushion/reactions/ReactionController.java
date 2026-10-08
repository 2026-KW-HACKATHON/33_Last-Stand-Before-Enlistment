package com.discushion.reactions;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController @Profile("!local")
final class ReactionController {
    private final ReactionService service;
    ReactionController(ReactionService service){this.service=service;}
    @PutMapping("/api/v1/posts/{postId}/reactions/{reactionType}")
    Map<String,Object> select(@PathVariable("postId")String id,@PathVariable("reactionType")String type,@RequestBody(required=false)String body) {
        return service.set(ReactionInput.parse(id,type,body),true);
    }
    @DeleteMapping("/api/v1/posts/{postId}/reactions/{reactionType}")
    Map<String,Object> cancel(@PathVariable("postId")String id,@PathVariable("reactionType")String type,@RequestBody(required=false)String body) {
        return service.set(ReactionInput.parse(id,type,body),false);
    }
}
