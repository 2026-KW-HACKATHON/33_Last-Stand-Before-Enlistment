package com.discushion.posts;

final class PostEditFailure extends RuntimeException {
    final String code;
    final int status;
    final String field;
    PostEditFailure(String code, int status, String field) {
        super(code); this.code = code; this.status = status; this.field = field;
    }
    static PostEditFailure invalid(String field) { return new PostEditFailure("VALIDATION_ERROR", 400, field); }
    static PostEditFailure typeInvalid(String field) { return new PostEditFailure("POST_TYPE_INVALID", 400, field); }
    static PostEditFailure notFound() { return new PostEditFailure("POST_NOT_FOUND", 404, "postId"); }
    static PostEditFailure notEditable() { return new PostEditFailure("POST_NOT_EDITABLE", 403, "postId"); }
    static PostEditFailure notDeletable() { return new PostEditFailure("POST_NOT_DELETABLE", 409, "postId"); }
}
