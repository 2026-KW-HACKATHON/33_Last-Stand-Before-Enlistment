package com.discushion.officer;

final class OfficerAgendaFailure extends RuntimeException {
    final String code;
    final String field;

    OfficerAgendaFailure(String code, String field) {
        super(code);
        this.code = code;
        this.field = field;
    }

    static OfficerAgendaFailure invalid(String field) {
        return new OfficerAgendaFailure("VALIDATION_ERROR", field);
    }
}
