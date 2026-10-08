package com.discushion.share;

final class ShareFailure extends RuntimeException {
    final int status;
    final String code;
    ShareFailure(int status, String code) { super(code); this.status = status; this.code = code; }
    static ShareFailure invalidToken() { return new ShareFailure(401, "UNAUTHORIZED"); }
    static ShareFailure missingPost() { return new ShareFailure(404, "POST_NOT_FOUND"); }
}
