package com.discushion.bookmark;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController @Profile("!local")
final class BookmarkController {
    private final BookmarkService service;
    BookmarkController(BookmarkService service){this.service=service;}
    @PutMapping("/api/v1/posts/{postId}/bookmark")
    Map<String,Object> add(@PathVariable String postId,@RequestBody(required=false)String body){
        if(body!=null&&!body.isBlank())throw BookmarkFailure.invalid("body");
        return service.set(BookmarkInput.postId(postId),true);
    }
    @DeleteMapping("/api/v1/posts/{postId}/bookmark")
    Map<String,Object> remove(@PathVariable String postId,@RequestBody(required=false)String body){
        if(body!=null&&!body.isBlank())throw BookmarkFailure.invalid("body");
        return service.set(BookmarkInput.postId(postId),false);
    }
    @GetMapping("/api/v1/users/me/bookmarks")
    BookmarkPage list(@RequestParam MultiValueMap<String,String> params){return service.list(BookmarkQuery.parse(params));}
}
