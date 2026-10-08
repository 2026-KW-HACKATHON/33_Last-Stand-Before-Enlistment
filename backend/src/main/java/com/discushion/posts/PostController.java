package com.discushion.posts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!local")
final class PostController {
    private final PostManagementService service;
    PostController(PostManagementService service) { this.service = service; }

    @PatchMapping("/api/v1/posts/{postId}")
    ResponseEntity<Map<String, Object>> patch(@PathVariable("postId") String postId, @RequestBody Map<String, Object> body) {
        var result = service.patch(PostIds.parse(postId, "postId"), body);
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("data", result.data());
        if (!result.removedFileIds().isEmpty())
            envelope.put("meta", Map.of("photoDeletion", Map.of("status", "PENDING", "fileIds", result.removedFileIds())));
        return ResponseEntity.ok(envelope);
    }

    @DeleteMapping("/api/v1/posts/{postId}")
    ResponseEntity<Void> delete(@PathVariable("postId") String postId, @RequestBody(required = false) String body) {
        if (body != null && !body.isBlank()) throw PostEditFailure.invalid("body");
        service.delete(PostIds.parse(postId, "postId"));
        return ResponseEntity.noContent().build();
    }
}
