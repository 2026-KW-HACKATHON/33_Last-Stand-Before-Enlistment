package com.discushion.share;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController @Profile("!local")
final class ShareController {
    private final ShareLinkService service;
    ShareController(ShareLinkService service) { this.service = service; }
    @GetMapping("/api/v1/posts/{postId}/share-link")
    Map<String, Object> issue(@PathVariable("postId") String id) { return Map.of("data", service.issue(id)); }
}
