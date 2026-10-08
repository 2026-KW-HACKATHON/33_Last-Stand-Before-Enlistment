package com.discushion.region;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Base64;

/** Versioned opaque position, bound to q. This public catalog cursor is not an authorization token. */
final class RegionCursor {
    private RegionCursor() {}
    record Position(String name, long id) {}

    static String encode(String q, Region last) {
        try {
            var bytes = new ByteArrayOutputStream();
            try (var out = new DataOutputStream(bytes)) {
                out.writeByte(1);
                out.writeUTF(q);
                out.writeUTF(Normalizer.normalize(last.name(), Normalizer.Form.NFC));
                out.writeLong(last.id());
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException failure) {
            throw new IllegalStateException("Region cursor encoding failed", failure);
        }
    }

    static Position decode(String value, String q) {
        if (value.isEmpty() || value.length() > 131_072 || !value.matches("[A-Za-z0-9_-]+")) {
            throw new RegionInputFailure("cursor");
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            if (!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(value)) {
                throw new RegionInputFailure("cursor");
            }
            try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (in.readUnsignedByte() != 1 || !in.readUTF().equals(q)) throw new RegionInputFailure("cursor");
                String name = in.readUTF();
                long id = in.readLong();
                if (name.isBlank() || !Normalizer.isNormalized(name, Normalizer.Form.NFC)
                        || id < 1 || id > Region.MAX_ID || in.available() != 0) {
                    throw new RegionInputFailure("cursor");
                }
                return new Position(name, id);
            }
        } catch (IOException | IllegalArgumentException failure) {
            throw new RegionInputFailure("cursor");
        }
    }
}
