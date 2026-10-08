package com.discushion.personal;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController @Profile("!local")
final class BoardController {
    private final BoardService service;
    BoardController(BoardService service){this.service=service;}
    @GetMapping("/api/v1/posts")
    Map<String,Object> list(@RequestParam MultiValueMap<String,String> params){return service.list(BoardQuery.parse(params));}
}
