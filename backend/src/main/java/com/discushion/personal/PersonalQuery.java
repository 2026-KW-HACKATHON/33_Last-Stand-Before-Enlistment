package com.discushion.personal;

import com.discushion.contracts.post.PostType;
import java.util.Set;
import org.springframework.util.MultiValueMap;

record PersonalQuery(Kind kind, PostType type, Status status, int size, String cursor) {
    enum Kind { POSTS, PARTICIPATIONS, VOTES }
    enum Status { ALL, OPEN, CLOSED }
    static PersonalQuery parse(Kind kind, MultiValueMap<String, String> params) {
        var allowed = kind == Kind.VOTES ? Set.of("status", "size", "cursor") : Set.of("type", "size", "cursor");
        if (!allowed.containsAll(params.keySet())) throw new PersonalFailure("query");
        PostType type = null; Status status = Status.ALL;
        String rawType = single(params, "type"), rawStatus = single(params, "status"), rawSize = single(params, "size");
        if (rawType != null) try { type = PostType.valueOf(rawType); } catch (IllegalArgumentException e) { throw new PersonalFailure("type"); }
        if (rawStatus != null) try { status = Status.valueOf(rawStatus); } catch (IllegalArgumentException e) { throw new PersonalFailure("status"); }
        int size = 20;
        if (rawSize != null) {
            if (!rawSize.matches("[0-9]{1,3}")) throw new PersonalFailure("size");
            size = Integer.parseInt(rawSize);
            if (size < 1 || size > 100) throw new PersonalFailure("size");
        }
        return new PersonalQuery(kind, type, status, size, single(params, "cursor"));
    }
    String filter() { return kind.name() + ":" + (type == null ? "ALL" : type.name()) + ":" + status.name(); }
    private static String single(MultiValueMap<String, String> params, String field) {
        var values = params.get(field);
        if (values == null) return null;
        if (values.size() != 1 || values.get(0) == null || values.get(0).isBlank()) throw new PersonalFailure(field);
        return values.get(0);
    }
}
