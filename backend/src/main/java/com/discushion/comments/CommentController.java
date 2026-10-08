package com.discushion.comments;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController @Profile("!local")
final class CommentController {
    private final CommentService service;
    CommentController(CommentService service) { this.service = service; }
    @GetMapping("/api/v1/posts/{postId}/comments")
    Map<String, Object> list(@PathVariable("postId") String value, @RequestParam MultiValueMap<String, String> params) {
        long id = CommentInput.pathId(value, "postId"); return service.list(id, CommentQuery.parse(id, params));
    }
    @PostMapping("/api/v1/posts/{postId}/comments")
    ResponseEntity<Map<String, Object>> create(@PathVariable("postId") String value, @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(201).body(service.create(CommentInput.pathId(value, "postId"), false, CommentInput.parse(body, false)));
    }
    @PostMapping("/api/v1/comments/{commentId}/replies")
    ResponseEntity<Map<String, Object>> reply(@PathVariable("commentId") String value, @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(201).body(service.create(CommentInput.pathId(value, "commentId"), true, CommentInput.parse(body, true)));
    }
}
