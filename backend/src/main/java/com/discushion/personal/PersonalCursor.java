package com.discushion.personal;

import java.io.*;
import java.time.Instant;
import java.util.Base64;

/** Position is scoped to the verified member and endpoint/filter; never grants access. */
final class PersonalCursor {
    record Position(Instant at, long postId) {}
    static String encode(long user, String filter, Position position) {
        try {
            var bytes = new ByteArrayOutputStream();
            try (var out = new DataOutputStream(bytes)) {
                out.writeByte(1); out.writeLong(user); out.writeUTF(filter);
                out.writeLong(position.at().getEpochSecond()); out.writeInt(position.at().getNano()); out.writeLong(position.postId());
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException e) { throw new IllegalStateException("Personal cursor encoding failed", e); }
    }
    static Position decode(String value, long user, String filter) {
        if (value.length() > 512 || !value.matches("[A-Za-z0-9_-]+")) throw new PersonalFailure("cursor");
        try {
            var bytes = Base64.getUrlDecoder().decode(value);
            if (!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(value)) throw new PersonalFailure("cursor");
            try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (in.readUnsignedByte() != 1 || in.readLong() != user || !in.readUTF().equals(filter)) throw new PersonalFailure("cursor");
                long seconds = in.readLong(); int nanos = in.readInt(); long id = in.readLong();
                if (nanos < 0 || nanos > 999_999_999 || id < 1 || id > 9007199254740991L || in.available() != 0) throw new PersonalFailure("cursor");
                return new Position(Instant.ofEpochSecond(seconds, nanos), id);
            }
        } catch (IOException | RuntimeException e) { throw new PersonalFailure("cursor"); }
    }
    private PersonalCursor() {}
}
