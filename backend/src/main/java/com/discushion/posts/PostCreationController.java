package com.discushion.posts;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!local & !share21-http-test & !comment22-http-test & !bookmark26-http-test & !reaction23-http-test & !vote25-http-test & !evaluation24-http-test")
final class PostCreationController {
    private final PostCreationService service;
    PostCreationController(PostCreationService service) { this.service = service; }

    @PostMapping("/api/v1/posts")
    ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(201).body(service.create(body));
    }
}
