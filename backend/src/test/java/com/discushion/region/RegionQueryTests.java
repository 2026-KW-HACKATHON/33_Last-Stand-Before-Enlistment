package com.discushion.region;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import static org.assertj.core.api.Assertions.*;

class RegionQueryTests {
    private RegionQuery query(String q, String size, String cursor) {
        var params = new LinkedMultiValueMap<String, String>();
        if (q != null) params.add("q", q);
        if (size != null) params.add("size", size);
        if (cursor != null) params.add("cursor", cursor);
        return RegionQuery.parse(params);
    }

    @Test void defaultsAndSearchNormalizationPreserveTheContract() {
        assertThat(query(null, null, null)).isEqualTo(new RegionQuery("", 20, null));
        assertThat(query("   ", null, null).q()).isEmpty();
        assertThat(query("  \u1112\u1161\u1100\u1168  ", "100", null).q()).isEqualTo("하계");
        assertThat(query("동".repeat(100), "1", null).size()).isEqualTo(1);
        assertThat(query("😀".repeat(100), null, null).q()).hasSize(200);
    }

    @Test void invalidSizesAreRejectedWithoutClamping() {
        for (String size : List.of("", "0", "101", "-1", "1.5", "+1", "2147483647", "abc")) {
            assertThatThrownBy(() -> query(null, size, null)).isInstanceOf(RegionInputFailure.class)
                .satisfies(error -> assertThat(((RegionInputFailure) error).field()).isEqualTo("size"));
        }
    }

    @Test void invalidSearchIsRejected() {
        for (String q : List.of("동".repeat(101), "가\u0000동", "가\n동")) {
            assertThatThrownBy(() -> query(q, null, null)).isInstanceOf(RegionInputFailure.class)
                .satisfies(error -> assertThat(((RegionInputFailure) error).field()).isEqualTo("q"));
        }
    }

    @Test void duplicateKnownParametersAreRejected() {
        for (String field : List.of("q", "size", "cursor")) {
            var params = new LinkedMultiValueMap<String, String>();
            params.add(field, "1"); params.add(field, "1");
            assertThatThrownBy(() -> RegionQuery.parse(params)).isInstanceOf(RegionInputFailure.class);
        }
    }

    @Test void cursorBindsNormalizedSearchAndAllowsPageSizeChange() {
        String value = RegionCursor.encode("하계", new Region(Region.MAX_ID, "하계2동", null));
        var parsed = query(" 하계 ", "100", value);
        assertThat(parsed.after()).isEqualTo(new RegionCursor.Position("하계2동", Region.MAX_ID));
        assertThatThrownBy(() -> query("다른지역", null, value)).isInstanceOf(RegionInputFailure.class);
    }

    @Test void malformedCursorIsRejected() {
        for (String cursor : List.of("", "invalid", "=", "%%%", "a".repeat(131_073))) {
            assertThatThrownBy(() -> query(null, null, cursor)).isInstanceOf(RegionInputFailure.class);
        }
        String valid = RegionCursor.encode("", new Region(1, "가동", null));
        assertThatThrownBy(() -> query(null, null, valid + "=")).isInstanceOf(RegionInputFailure.class);
    }

    @Test void cursorVersionTrailingBytesAndUnsafeIdAreRejected() throws Exception {
        String valid = RegionCursor.encode("", new Region(1, "가동", null));
        byte[] bytes = Base64.getUrlDecoder().decode(valid);
        bytes[0] = 2;
        assertThatThrownBy(() -> query(null, null, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)))
            .isInstanceOf(RegionInputFailure.class);
        var extra = new ByteArrayOutputStream(); extra.write(Base64.getUrlDecoder().decode(valid)); extra.write(0);
        assertThatThrownBy(() -> query(null, null, Base64.getUrlEncoder().withoutPadding().encodeToString(extra.toByteArray())))
            .isInstanceOf(RegionInputFailure.class);
        for (long id : new long[]{0, -1, Region.MAX_ID + 1}) {
            assertThatThrownBy(() -> query(null, null, RegionCursor.encode("", new Region(id, "가동", null))))
                .isInstanceOf(RegionInputFailure.class);
        }
    }

    @Test void cursorRequiresNormalizedNonblankName() throws Exception {
        for (String name : List.of(" ", "\u1100\u1161동")) {
            var bytes = new ByteArrayOutputStream();
            try (var out = new DataOutputStream(bytes)) {
                out.writeByte(1);out.writeUTF("");out.writeUTF(name);out.writeLong(1);
            }
            String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
            assertThatThrownBy(() -> query(null, null, value)).isInstanceOf(RegionInputFailure.class);
        }
    }
}
