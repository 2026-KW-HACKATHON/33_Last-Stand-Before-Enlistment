package com.discushion.photos;

import java.util.Map;
import java.io.InputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static com.discushion.photos.PhotoFailure.Reason.*;

@RestController
@RequestMapping("/api/v1/photo-uploads")
@ConditionalOnProperty(name="PHOTO_UPLOADS_ENABLED",havingValue="true")
public final class PhotoController {
    private final PhotoService service;
    public PhotoController(PhotoService service) {this.service=service;}
    @PostMapping
    ResponseEntity<?> reserve(@RequestBody Map<String,Object> body) {
        if(body==null || !body.keySet().equals(java.util.Set.of("originalName","contentType","sizeBytes"))
            || !(body.get("originalName") instanceof String name) || !(body.get("contentType") instanceof String mime)
            || !(body.get("sizeBytes") instanceof Number size) || !(size instanceof Integer || size instanceof Long))
            throw new PhotoFailure(VALIDATION_ERROR);
        return ResponseEntity.status(201).body(Map.of("data",service.reserve(name,mime,size.longValue())));
    }
    @PostMapping("/{fileId}/complete")
    Map<String,Object> complete(@PathVariable long fileId,@RequestBody Map<String,Object> body) {
        if(body==null || !body.isEmpty()) throw new PhotoFailure(VALIDATION_ERROR);
        return Map.of("data",service.complete(fileId));
    }
    @PutMapping("/{fileId}/content")
    Map<String,Object> upload(@PathVariable long fileId,@RequestHeader(value="Content-Type",required=false) String mime,InputStream body) {
        return Map.of("data",service.upload(fileId,mime,body));
    }
    @GetMapping("/{fileId}")
    Map<String,Object> get(@PathVariable long fileId) {return Map.of("data",service.get(fileId));}
    @DeleteMapping("/{fileId}")
    ResponseEntity<?> cancel(@PathVariable long fileId,@RequestBody(required=false) String body) {
        if(body!=null && !body.isBlank()) throw new PhotoFailure(VALIDATION_ERROR);
        var view=service.cancel(fileId);
        return ResponseEntity.status(view.deletionCompleted()?200:202).body(Map.of("data",view));
    }
}
