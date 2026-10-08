package com.discushion.summary;
final class SummaryFailure extends RuntimeException {
    final int status;final String code;
    SummaryFailure(int status,String code){super(code);this.status=status;this.code=code;}
}
