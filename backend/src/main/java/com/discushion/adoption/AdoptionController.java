package com.discushion.adoption;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!local")
final class AdoptionController {
    private final AdoptionService service;

    AdoptionController(AdoptionService service) { this.service = service; }

    @PostMapping("/api/v1/posts/{postId}/adoptions")
    ResponseEntity<Map<String, Object>> adopt(@PathVariable("postId") String postId,
            @RequestBody(required = false) String body) {
        if (body != null && !body.isBlank()) throw AdoptionFailure.invalid("body");
        var result = service.adopt(AdoptionIds.parse(postId, "postId"));
        return ResponseEntity.status(result.created() ? 201 : 200).body(Map.of("data", result.adoption()));
    }

    @DeleteMapping("/api/v1/posts/{postId}/adoptions/{adoptionId}")
    ResponseEntity<Void> cancel(@PathVariable("postId") String postId, @PathVariable("adoptionId") String adoptionId,
            @RequestBody(required = false) String body) {
        if (body != null && !body.isBlank()) throw AdoptionFailure.invalid("body");
        service.cancel(AdoptionIds.parse(postId, "postId"), AdoptionIds.parse(adoptionId, "adoptionId"));
        return ResponseEntity.noContent().build();
    }
}
