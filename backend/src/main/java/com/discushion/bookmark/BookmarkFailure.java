package com.discushion.bookmark;

final class BookmarkFailure extends RuntimeException {
    final String code, field;
    final int status;
    BookmarkFailure(String code, int status, String field) { super(code); this.code=code; this.status=status; this.field=field; }
    static BookmarkFailure invalid(String field) { return new BookmarkFailure("VALIDATION_ERROR",400,field); }
}
