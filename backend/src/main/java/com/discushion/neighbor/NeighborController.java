package com.discushion.neighbor;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController @Profile("!local")
final class NeighborController {
    private final NeighborService service;
    NeighborController(NeighborService service) {this.service=service;}
    @GetMapping("/api/v1/users/me/neighbor-verifications")
    Map<String,Object> current(@RequestParam(name="regionId",required=false) String regionId) {return Map.of("data",service.current(regionId));}
}
