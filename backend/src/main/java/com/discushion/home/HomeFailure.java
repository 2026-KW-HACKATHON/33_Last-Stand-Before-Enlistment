package com.discushion.home;

final class HomeFailure extends RuntimeException {
    final int status; final String code; final String field;
    HomeFailure(int status,String code,String field){super(code);this.status=status;this.code=code;this.field=field;}
    static HomeFailure invalid(String field){return new HomeFailure(400,"VALIDATION_ERROR",field);}
}
