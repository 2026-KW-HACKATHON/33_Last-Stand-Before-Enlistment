package com.discushion.personal;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController @Profile("!local")
final class PersonalController {
    private final PersonalService service;
    PersonalController(PersonalService service) { this.service = service; }
    @GetMapping("/api/v1/users/me/posts")
    Map<String, Object> posts(@RequestParam MultiValueMap<String, String> params) { return service.list(PersonalQuery.parse(PersonalQuery.Kind.POSTS, params)); }
    @GetMapping("/api/v1/users/me/participations")
    Map<String, Object> participations(@RequestParam MultiValueMap<String, String> params) { return service.list(PersonalQuery.parse(PersonalQuery.Kind.PARTICIPATIONS, params)); }
    @GetMapping("/api/v1/users/me/votes")
    Map<String, Object> votes(@RequestParam MultiValueMap<String, String> params) { return service.list(PersonalQuery.parse(PersonalQuery.Kind.VOTES, params)); }
}
