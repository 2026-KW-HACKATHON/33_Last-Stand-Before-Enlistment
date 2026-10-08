package com.discushion.officer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/** Opaque keyset position bound to the current institution and list filters. */
final class OfficerAgendaCursor {
    private OfficerAgendaCursor() {}

    record Filter(long institutionId, OfficerAgendaScope scope, Long regionId) {}
    record Position(long reactionCount, Instant createdAt, long postId) {}

    static String encode(Filter filter, Position position) {
        try {
            var bytes = new ByteArrayOutputStream();
            try (var out = new DataOutputStream(bytes)) {
                out.writeByte(1);
                out.writeLong(filter.institutionId());
                out.writeUTF(filter.scope().name());
                out.writeBoolean(filter.regionId() != null);
                if (filter.regionId() != null) out.writeLong(filter.regionId());
                out.writeLong(position.reactionCount());
                out.writeLong(position.createdAt().getEpochSecond());
                out.writeInt(position.createdAt().getNano());
                out.writeLong(position.postId());
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException failure) {
            throw new IllegalStateException("Officer agenda cursor encoding failed", failure);
        }
    }

    static Position decode(String value, Filter filter) {
        if (value.isEmpty() || value.length() > 512 || !value.matches("[A-Za-z0-9_-]+")) {
            throw OfficerAgendaFailure.invalid("cursor");
        }
        try {
            var bytes = Base64.getUrlDecoder().decode(value);
            if (!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(value)) {
                throw OfficerAgendaFailure.invalid("cursor");
            }
            try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (in.readUnsignedByte() != 1
                        || in.readLong() != filter.institutionId()
                        || !Objects.equals(in.readUTF(), filter.scope().name())) {
                    throw OfficerAgendaFailure.invalid("cursor");
                }
                Long regionId = in.readBoolean() ? in.readLong() : null;
                long reactionCount = in.readLong();
                long seconds = in.readLong();
                int nanos = in.readInt();
                long postId = in.readLong();
                if (!Objects.equals(regionId, filter.regionId()) || reactionCount < 0
                        || nanos < 0 || nanos > 999_999_999 || postId < 1
                        || postId > 9_007_199_254_740_991L || in.available() != 0) {
                    throw OfficerAgendaFailure.invalid("cursor");
                }
                return new Position(reactionCount, Instant.ofEpochSecond(seconds, nanos), postId);
            }
        } catch (IOException | RuntimeException failure) {
            if (failure instanceof OfficerAgendaFailure invalid) throw invalid;
            throw OfficerAgendaFailure.invalid("cursor");
        }
    }
}
