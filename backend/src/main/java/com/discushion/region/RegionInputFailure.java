package com.discushion.region;

final class RegionInputFailure extends RuntimeException {
    private final String field;

    RegionInputFailure(String field) {
        super("지역 조회 입력을 확인해 주세요.");
        this.field = field;
    }

    String field() { return field; }
}
