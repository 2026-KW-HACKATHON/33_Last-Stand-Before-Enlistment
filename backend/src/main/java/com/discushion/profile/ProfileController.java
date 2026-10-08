package com.discushion.profile;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController @Profile("!local")
final class ProfileController {
    private final ProfileService service;
    ProfileController(ProfileService service) {this.service=service;}
    @GetMapping("/api/v1/users/me") Map<String,Object> current() {return Map.of("data",service.current());}
    @PatchMapping("/api/v1/users/me") Map<String,Object> patch(@RequestBody Map<String,Object> body) {
        return Map.of("data",service.patch(ProfilePatch.parse(body)));
    }
}
