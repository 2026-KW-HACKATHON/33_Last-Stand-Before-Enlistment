package com.discushion.map;
final class MapFailure extends RuntimeException {
    final int status; final String code,field;
    MapFailure(int status,String code,String field){super(code);this.status=status;this.code=code;this.field=field;}
    static MapFailure invalid(String field){return new MapFailure(400,"VALIDATION_ERROR",field);}
}
