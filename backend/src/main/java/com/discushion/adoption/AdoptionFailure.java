package com.discushion.adoption;

final class AdoptionFailure extends RuntimeException {
    final String code;
    final int status;
    final String field;

    AdoptionFailure(String code, int status, String field) {
        super(code);
        this.code = code;
        this.status = status;
        this.field = field;
    }

    static AdoptionFailure invalid(String field) { return new AdoptionFailure("VALIDATION_ERROR", 400, field); }
    static AdoptionFailure notAllowed() { return new AdoptionFailure("ADOPTION_NOT_ALLOWED", 403, "postId"); }
    static AdoptionFailure postNotFound() { return new AdoptionFailure("POST_NOT_FOUND", 404, "postId"); }
    static AdoptionFailure adoptionNotFound() { return new AdoptionFailure("ADOPTION_NOT_FOUND", 404, "adoptionId"); }
}
