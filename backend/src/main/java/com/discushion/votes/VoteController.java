package com.discushion.votes;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController @Profile("!local")
final class VoteController {
    private final VoteService service;
    VoteController(VoteService service){this.service=service;}

    @PutMapping("/api/v1/posts/{postId}/vote")
    Map<String,Object> submit(@PathVariable("postId") String id,@RequestBody(required=false) Map<String,Object> body) {
        return service.submit(VoteInput.parse(id,body));
    }
}
