package com.discushion.profile;

final class ProfileFailure extends RuntimeException {
    final String code,field; final int status;
    ProfileFailure(String code,int status,String field) {super(code);this.code=code;this.status=status;this.field=field;}
    static ProfileFailure invalid(String field) {return new ProfileFailure("VALIDATION_ERROR",400,field);}
}
