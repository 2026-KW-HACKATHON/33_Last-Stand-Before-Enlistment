package com.discushion.institution;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!local")
final class InstitutionController {
    private final InstitutionLookup lookup;

    InstitutionController(InstitutionLookup lookup) { this.lookup = lookup; }

    @GetMapping("/api/v1/institution-verifications")
    Map<String, Object> current() { return Map.of("data", InstitutionView.from(lookup.current())); }
}
