package com.discushion.comments;

/** Feature errors use the existing shared envelope and established codes. */
final class CommentFailure extends RuntimeException {
    final String code, field;
    final int status;
    CommentFailure(String code, int status, String field) {
        super(code); this.code = code; this.status = status; this.field = field;
    }
    static CommentFailure invalid(String field) { return new CommentFailure("VALIDATION_ERROR", 400, field); }
}
