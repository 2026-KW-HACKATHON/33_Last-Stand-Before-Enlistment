package com.discushion.summary;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
@RestController @Profile("!local")
final class SummaryController {
    private final SummaryService service;
    SummaryController(SummaryService service){this.service=service;}
    @GetMapping("/api/v1/posts/{postId}/summary")
    ResponseEntity<Map<String,Object>> read(@PathVariable String postId,@RequestParam MultiValueMap<String,String> params){
        if(!params.isEmpty() || !postId.matches("[1-9][0-9]{0,15}") || Long.parseLong(postId)>9007199254740991L)
            throw new SummaryFailure(400,"VALIDATION_ERROR");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.read(Long.parseLong(postId)));
    }
}
