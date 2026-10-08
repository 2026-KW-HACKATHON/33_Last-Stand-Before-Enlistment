package com.discushion.officer;

import org.springframework.context.annotation.Profile;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!local")
final class OfficerAgendaController {
    private final OfficerAgendaService service;
    OfficerAgendaController(OfficerAgendaService service) { this.service = service; }

    @GetMapping("/api/v1/officer/agendas")
    OfficerAgendaPage list(@RequestParam MultiValueMap<String, String> params) {
        return service.list(OfficerAgendaQuery.parse(params));
    }
}
