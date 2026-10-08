package com.discushion.posts;

final class PostDetailFailure extends RuntimeException {
    final String code;
    final int status;
    PostDetailFailure(String code, int status) { super(code); this.code = code; this.status = status; }
    static PostDetailFailure missing() { return new PostDetailFailure("POST_NOT_FOUND", 404); }
}
