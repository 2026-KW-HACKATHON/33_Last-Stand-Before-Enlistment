package com.discushion.region;

import java.text.Normalizer;
import org.springframework.util.MultiValueMap;

record RegionQuery(String q, int size, RegionCursor.Position after) {
    static RegionQuery parse(MultiValueMap<String, String> params) {
        String q = single(params, "q");
        q = q == null ? "" : Normalizer.normalize(q.strip(), Normalizer.Form.NFC);
        if (q.codePointCount(0, q.length()) > 100 || q.codePoints().anyMatch(Character::isISOControl)) {
            throw new RegionInputFailure("q");
        }
        String rawSize = single(params, "size");
        int size = 20;
        if (rawSize != null) {
            if (!rawSize.matches("[0-9]{1,3}")) throw new RegionInputFailure("size");
            size = Integer.parseInt(rawSize);
            if (size < 1 || size > 100) throw new RegionInputFailure("size");
        }
        String cursor = single(params, "cursor");
        return new RegionQuery(q, size, cursor == null ? null : RegionCursor.decode(cursor, q));
    }

    private static String single(MultiValueMap<String, String> params, String field) {
        var values = params.get(field);
        if (values == null) return null;
        if (values.size() != 1 || values.get(0) == null) throw new RegionInputFailure(field);
        return values.get(0);
    }
}
