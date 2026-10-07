package com.discushion.signup;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!local")
final class SignupController {
    private final SignupService service;
    SignupController(SignupService service) { this.service=service; }
    @PostMapping("/api/v1/auth/sign-up")
    ResponseEntity<?> complete(@RequestBody Map<String,Object> body) {
        var outcome=service.complete(SignupInput.parse(body));
        return ResponseEntity.status(outcome.completedNow()?201:200).body(Map.of("data",outcome.result()));
    }
}
