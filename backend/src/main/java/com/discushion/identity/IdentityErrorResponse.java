package com.discushion.identity;

import java.util.List;
import java.util.UUID;

/** The established error envelope; no token, subject, provider message or DB error is returned. */
public record IdentityErrorResponse(String code, String message, List<Object> details, String traceId) {
    public static IdentityErrorResponse internal() {
        return new IdentityErrorResponse("INTERNAL_ERROR", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
            List.of(), UUID.randomUUID().toString());
    }

    public static IdentityErrorResponse from(IdentityFailure failure) {
        var code = switch (failure.reason()) {
            case INVALID_TOKEN -> "UNAUTHORIZED";
            case NOT_REGISTERED, INCOMPLETE -> "USER_REGISTRATION_REQUIRED";
            case PROVIDER_UNAVAILABLE -> "AUTH_PROVIDER_UNAVAILABLE";
            case MEMBER_NOT_FOUND -> "USER_NOT_FOUND";
            case REGION_REQUIRED -> "NEIGHBOR_VERIFICATION_REQUIRED";
            case INSTITUTION_REQUIRED -> "INSTITUTION_VERIFICATION_NOT_ACTIVE";
        };
        var message = switch (failure.reason()) {
            case INVALID_TOKEN -> "로그인이 필요한 기능입니다.";
            case NOT_REGISTERED, INCOMPLETE -> "회원가입을 완료해 주세요.";
            case PROVIDER_UNAVAILABLE -> "인증 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.";
            case MEMBER_NOT_FOUND -> "회원을 찾을 수 없습니다.";
            case REGION_REQUIRED -> "해당 지역의 이웃 인증이 필요합니다.";
            case INSTITUTION_REQUIRED -> "유효한 기관 자격이 필요합니다.";
        };
        return new IdentityErrorResponse(code, message, List.of(), UUID.randomUUID().toString());
    }

    public static int status(IdentityFailure failure) {
        return switch (failure.reason()) {
            case INVALID_TOKEN -> 401;
            case PROVIDER_UNAVAILABLE -> 503;
            case MEMBER_NOT_FOUND -> 404;
            default -> 403;
        };
    }
}
