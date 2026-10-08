package com.discushion.personal;

final class PersonalFailure extends RuntimeException {
    final String field;
    PersonalFailure(String field) { super("VALIDATION_ERROR"); this.field = field; }
}
