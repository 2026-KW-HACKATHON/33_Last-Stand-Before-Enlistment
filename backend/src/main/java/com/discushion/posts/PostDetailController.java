package com.discushion.posts;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("!local & !share21-http-test & !comment22-http-test & !bookmark26-http-test & !reaction23-http-test & !vote25-http-test & !evaluation24-http-test & !officer28-http-test")
final class PostDetailController {
    private final PostDetailService details;
    PostDetailController(PostDetailService details) { this.details = details; }
    @GetMapping("/api/v1/posts/{postId}")
    Map<String, Object> get(@PathVariable("postId") String value, @RequestParam MultiValueMap<String, String> query) {
        if (!query.isEmpty() || !value.matches("[1-9][0-9]{0,15}")) throw new PostDetailFailure("VALIDATION_ERROR", 400);
        long id = Long.parseLong(value);
        if (id > 9007199254740991L) throw new PostDetailFailure("VALIDATION_ERROR", 400);
        return Map.of("data", details.get(id));
    }
}
