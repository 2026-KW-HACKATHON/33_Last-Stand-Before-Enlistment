package com.discushion.home;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.context.annotation.Profile;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController @Profile("!local")
final class HomeController {
    private final HomeService service;
    HomeController(HomeService service){this.service=service;}
    @GetMapping("/api/v1/home")
    ResponseEntity<Map<String,Object>> home(@RequestParam MultiValueMap<String,String> params){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.read(HomeQuery.parse(params)));}
}
