package com.discushion.login;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!local")
final class LoginController {
    private final LoginService service;
    LoginController(LoginService service) { this.service=service; }
    @PostMapping("/api/v1/auth/login")
    Map<String,Object> login(@RequestBody Map<String,Object> body) {
        if (body==null || !body.isEmpty()) throw new LoginInputFailure();
        return Map.of("data",service.current());
    }
}
