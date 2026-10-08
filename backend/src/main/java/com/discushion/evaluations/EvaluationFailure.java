package com.discushion.evaluations;

final class EvaluationFailure extends RuntimeException {
    final String code, field; final int status;
    EvaluationFailure(String code,int status,String field){super(code);this.code=code;this.status=status;this.field=field;}
    static EvaluationFailure invalid(String field){return new EvaluationFailure("VALIDATION_ERROR",400,field);}
}
