package com.discushion.evaluations;

import java.util.Map;
import java.util.Set;

/** Agreed types and JSON-safe target only; no client user, role or toggle state. */
record EvaluationInput(long commentId, Type type) {
    enum Type { LIKE, DISLIKE }
    static EvaluationInput select(String id,Map<String,Object> body) {
        long target=pathId(id);
        if(body==null || !body.keySet().equals(Set.of("type")))throw EvaluationFailure.invalid("body");
        if(!(body.get("type") instanceof String value))throw invalidType();
        try{return new EvaluationInput(target,Type.valueOf(value));}
        catch(IllegalArgumentException error){throw invalidType();}
    }
    static long pathId(String id) {
        if(id==null || !id.matches("[1-9][0-9]{0,15}"))throw EvaluationFailure.invalid("commentId");
        try {
            long value=Long.parseLong(id);
            if(value>9007199254740991L)throw EvaluationFailure.invalid("commentId");
            return value;
        } catch(NumberFormatException error){throw EvaluationFailure.invalid("commentId");}
    }
    private static EvaluationFailure invalidType(){return new EvaluationFailure("COMMENT_EVALUATION_TYPE_INVALID",400,"type");}
}
