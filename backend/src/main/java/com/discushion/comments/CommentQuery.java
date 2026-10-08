package com.discushion.comments;

import java.io.*;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import org.springframework.util.MultiValueMap;

record CommentQuery(Sort sort, int size, Position after) {
    enum Sort { LIKES, LATEST }
    record Position(long likes, Instant createdAt, long id) {}
    static CommentQuery parse(long postId, MultiValueMap<String, String> params) {
        if (!Set.of("sort", "size", "cursor").containsAll(params.keySet())) throw CommentFailure.invalid("query");
        String value = single(params, "sort");
        Sort sort;
        try { sort = value == null ? Sort.LIKES : Sort.valueOf(value); }
        catch (IllegalArgumentException invalid) { throw CommentFailure.invalid("sort"); }
        int size = 20; value = single(params, "size");
        if (value != null) {
            if (!value.matches("[0-9]{1,3}")) throw CommentFailure.invalid("size");
            size = Integer.parseInt(value);
            if (size < 1 || size > 100) throw CommentFailure.invalid("size");
        }
        value = single(params, "cursor");
        return new CommentQuery(sort, size, value == null ? null : decode(value, postId, sort));
    }
    static String encode(long postId, Sort sort, Position p) {
        try {
            var bytes = new ByteArrayOutputStream();
            try (var out = new DataOutputStream(bytes)) {
                out.writeByte(1); out.writeLong(postId); out.writeUTF(sort.name());
                out.writeLong(p.likes()); out.writeLong(p.createdAt().getEpochSecond());
                out.writeInt(p.createdAt().getNano()); out.writeLong(p.id());
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException error) { throw new IllegalStateException("Comment cursor encoding failed"); }
    }
    private static Position decode(String value, long postId, Sort sort) {
        if (value.isEmpty() || value.length() > 128 || !value.matches("[A-Za-z0-9_-]+")) throw CommentFailure.invalid("cursor");
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            if (!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(value)) throw CommentFailure.invalid("cursor");
            try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (in.readUnsignedByte() != 1 || in.readLong() != postId || !in.readUTF().equals(sort.name())) throw CommentFailure.invalid("cursor");
                long likes = in.readLong(), seconds = in.readLong(); int nanos = in.readInt(); long id = in.readLong();
                if (likes < 0 || nanos < 0 || nanos >= 1_000_000_000 || id < 1 || id > 9007199254740991L || in.available() != 0)
                    throw CommentFailure.invalid("cursor");
                return new Position(likes, Instant.ofEpochSecond(seconds, nanos), id);
            }
        } catch (IOException | IllegalArgumentException | java.time.DateTimeException error) { throw CommentFailure.invalid("cursor"); }
    }
    private static String single(MultiValueMap<String, String> params, String field) {
        var values = params.get(field);
        if (values == null) return null;
        if (values.size() != 1 || values.get(0) == null) throw CommentFailure.invalid(field);
        return values.get(0);
    }
}
