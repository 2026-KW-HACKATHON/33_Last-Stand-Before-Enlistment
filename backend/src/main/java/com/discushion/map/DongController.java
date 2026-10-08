package com.discushion.map;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.context.annotation.Profile;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
@RestController @Profile("!local")
final class DongController {
    private final DongService service;
    DongController(DongService service){this.service=service;}
    @GetMapping("/api/v1/map/dongs")
    ResponseEntity<Map<String,Object>> read(@RequestParam MultiValueMap<String,String> params){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.read(DongQuery.parse(params)));}
}
